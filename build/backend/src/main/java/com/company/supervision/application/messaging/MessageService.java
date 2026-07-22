package com.company.supervision.application.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.supervision.application.organization.WecomIntegrationService;
import com.company.supervision.domain.model.messaging.*;
import com.company.supervision.infrastructure.repository.messaging.*;
import com.company.supervision.infrastructure.security.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class MessageService {
    private static final Set<String>TYPES=Set.of("text","markdown","markdown_v2","image","news","file","voice","template_card");
    private final MessageDeliveryMapper deliveries; private final WecomWebhookMapper hooks; private final WecomGroupMapper groups;
    private final SecretCipher cipher; private final StringRedisTemplate redis; private final RestTemplate http; private final ObjectMapper json; private final WecomIntegrationService wecom;
    public MessageService(MessageDeliveryMapper d,WecomWebhookMapper h,WecomGroupMapper g,SecretCipher c,StringRedisTemplate r,RestTemplate t,ObjectMapper j,WecomIntegrationService w){deliveries=d;hooks=h;groups=g;cipher=c;redis=r;http=t;json=j;wecom=w;}

    public Map<String,Object>preview(Map<String,Object>req){validate(req);Map<String,Object>r=new LinkedHashMap<>();r.put("channel",req.getOrDefault("channel","GROUP_WEBHOOK"));r.put("messageType",req.get("messageType"));r.put("targetCount",targetIds(req).size());r.put("payload",payload(req));r.put("warning","GROUP_WEBHOOK".equals(r.get("channel"))?"所选组织成员不一定属于目标群":null);return r;}
    public List<MessageDelivery>test(Map<String,Object>req){req=new HashMap<>(req);req.put("idempotencyKey","test-"+UUID.randomUUID());return sendAll(req);}

    public List<MessageDelivery>sendAll(Map<String,Object>req){
        validate(req);
        if(!"GROUP_WEBHOOK".equals(String.valueOf(req.getOrDefault("channel","GROUP_WEBHOOK"))))return List.of(send(req));
        List<Long> ids=targetIds(req); if(ids.isEmpty())throw new IllegalArgumentException("请至少选择一个目标群");
        List<MessageDelivery>out=new ArrayList<>();String base=String.valueOf(req.getOrDefault("idempotencyKey",UUID.randomUUID().toString()));
        for(Long id:ids){Map<String,Object>one=new HashMap<>(req);one.put("webhookId",id);one.put("idempotencyKey",base+"-webhook-"+id);out.add(send(one));}
        return out;
    }

    public MessageDelivery send(Map<String,Object>req){
        validate(req);String key=String.valueOf(req.getOrDefault("idempotencyKey",UUID.randomUUID().toString()));MessageDelivery existing=deliveries.byKey(key);if(existing!=null)return existing;
        MessageDelivery d=new MessageDelivery();d.setTaskId(longValue(req.get("taskId")));d.setExecutionId(longValue(req.get("executionId")));d.setChannel(String.valueOf(req.getOrDefault("channel","GROUP_WEBHOOK")));d.setMessageType(String.valueOf(req.get("messageType")));d.setTargetSnapshot(write(req.get("recipients")));d.setIdempotencyKey(key);d.setStatus("PENDING");d.setRetryCount(0);d.setContentSummarySnapshot(summary(req));
        Long targetId="GROUP_WEBHOOK".equals(d.getChannel())?longValue(req.get("webhookId")):null;d.setWebhookId(targetId);
        if(targetId!=null){WecomWebhook h=hooks.selectById(targetId);WecomGroup g=h==null?null:groups.selectById(h.getGroupId());if(h!=null){d.setPushNameSnapshot(first(h.getPushName(),h.getName()));d.setGroupIdSnapshot(h.getGroupId());}if(g!=null)d.setGroupNameSnapshot(g.getGroupName());}
        deliveries.insert(d);
        try{
            JsonNode response;
            if("GROUP_WEBHOOK".equals(d.getChannel())){WecomWebhook h=hooks.selectById(targetId);if(h==null||!Objects.equals(h.getStatus(),1)||h.getWebhookCipher()==null)throw new IllegalArgumentException("TARGET_UNAVAILABLE");WecomGroup g=groups.selectById(h.getGroupId());if(g==null||!Objects.equals(g.getStatus(),1))throw new IllegalArgumentException("TARGET_UNAVAILABLE");rateLimit(targetId);response=post(cipher.decrypt(h.getWebhookCipher()),payload(req));}
            else{String token=wecom.token(false);Map<String,Object>p=payload(req);p.put("agentid",wecom.agentId());response=post("https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token="+token,p);}
            d.setWecomErrcode(response.path("errcode").asInt(-1));if(d.getWecomErrcode()!=0)throw new IllegalStateException("WECOM_"+d.getWecomErrcode()+":"+response.path("errmsg").asText("WeCom error"));
            d.setStatus("SUCCESS");d.setNormalizedCode("SUCCESS");d.setNormalizedMessage("推送成功");d.setSentAt(LocalDateTime.now());
        }catch(Exception e){String raw=SensitiveDataRedactor.redact(e.getMessage());String code=normalizeCode(raw);d.setStatus("RATE_LIMITED".equals(code)?"RATE_LIMITED":"FAILED");d.setFailureReason(readable(code));d.setNormalizedCode(code);d.setNormalizedMessage(readable(code));d.setTechnicalDetailRedacted(raw);}
        deliveries.updateById(d);return d;
    }

    public List<MessageDelivery>list(){return deliveries.selectList(new LambdaQueryWrapper<MessageDelivery>().orderByDesc(MessageDelivery::getId).last("LIMIT 200"));}
    public List<MessageDelivery>byExecution(Long executionId){return deliveries.selectList(new LambdaQueryWrapper<MessageDelivery>().eq(MessageDelivery::getExecutionId,executionId).orderByAsc(MessageDelivery::getId));}

    @SuppressWarnings("unchecked") Map<String,Object>payload(Map<String,Object>req){String type=String.valueOf(req.get("messageType"));Object content=req.get("content");Map<String,Object>raw=content instanceof Map?(Map<String,Object>)content:new LinkedHashMap<>(Map.of("content",content==null?"":content.toString()));Map<String,Object>body=new LinkedHashMap<>();if(Set.of("text","markdown","markdown_v2").contains(type))body.put("content",raw.getOrDefault("content",""));else if("news".equals(type))body.put("articles",raw.getOrDefault("articles",List.of()));else if(Set.of("file","voice").contains(type))body.put("media_id",raw.get("media_id"));else if("image".equals(type)){body.put("base64",raw.get("base64"));body.put("md5",raw.get("md5"));}else{body.putAll(raw);body.putIfAbsent("card_type","text_notice");}Object mode=req.get("mentionMode");if("ALL".equals(mode)){if("markdown_v2".equals(type))throw new IllegalArgumentException("markdown_v2 does not support mentions");if("text".equals(type))body.put("mentioned_list",List.of("@all"));else body.put("content",body.getOrDefault("content","")+" <@all>");}else if("SELECTED".equals(mode)){List<String>ids=(List<String>)req.getOrDefault("mentionedUserIds",List.of());if("markdown_v2".equals(type))throw new IllegalArgumentException("markdown_v2 does not support mentions");if("text".equals(type))body.put("mentioned_list",ids);else{StringBuilder s=new StringBuilder(String.valueOf(body.getOrDefault("content","")));ids.forEach(i->s.append(" <@").append(i).append(">"));body.put("content",s.toString());}}Map<String,Object>p=new LinkedHashMap<>();p.put("msgtype",type);p.put(type,body);if("APP_MESSAGE".equals(req.get("channel"))){p.put("touser",join(req.get("userIds")));p.put("toparty",join(req.get("departmentIds")));p.put("totag",join(req.get("tagIds")));}return p;}
    void validate(Map<String,Object>r){String type=String.valueOf(r.get("messageType"));if(!TYPES.contains(type))throw new IllegalArgumentException("Unsupported message type: "+type);if("news".equals(type)&&r.get("content") instanceof Map){Object a=((Map<?,?>)r.get("content")).get("articles");if(a instanceof Collection&&(((Collection<?>)a).isEmpty()||((Collection<?>)a).size()>8))throw new IllegalArgumentException("News requires 1 to 8 articles");}}
    @SuppressWarnings("unchecked") public List<Long>targetIds(Map<String,Object>r){LinkedHashSet<Long>out=new LinkedHashSet<>();Object many=r.get("webhookIds");if(many instanceof Collection<?>)for(Object v:(Collection<Object>)many){Long id=longValue(v);if(id!=null)out.add(id);}Long legacy=longValue(r.get("webhookId"));if(legacy!=null)out.add(legacy);return new ArrayList<>(out);}
    private String summary(Map<String,Object>r){Object c=r.get("content");Object contentValue=c instanceof Map?((Map<?,?>)c).get("content"):c;String v=String.valueOf(contentValue==null?"":contentValue);return v.length()>500?v.substring(0,500)+"…":v;}
    private String normalizeCode(String raw){if(raw==null)return"UNKNOWN";String x=raw.toLowerCase();if(x.contains("rate")||x.contains("45009"))return"RATE_LIMITED";if(x.contains("target_unavailable")||x.contains("webhook"))return"TARGET_UNAVAILABLE";if(x.contains("timeout"))return"NETWORK_TIMEOUT";if(x.contains("40014")||x.contains("42001")||x.contains("permission"))return"AUTH_OR_PERMISSION";if(x.contains("unsupported")||x.contains("invalid"))return"CONTENT_INVALID";return"UNKNOWN";}
    private String readable(String code){return switch(code){case"RATE_LIMITED"->"企微发送频率受限，请稍后重试";case"TARGET_UNAVAILABLE"->"目标群已停用或消息推送配置无效";case"NETWORK_TIMEOUT"->"连接企微超时，请稍后重试";case"AUTH_OR_PERMISSION"->"企微凭据失效或应用权限不足";case"CONTENT_INVALID"->"消息内容格式不受支持";default->"推送失败，请查看技术详情或联系管理员";};}
    private void rateLimit(Long id){String k="supervision:wecom:webhook-rate:"+id+":"+LocalDateTime.now().withSecond(0).withNano(0);Long n=redis.opsForValue().increment(k);if(n!=null&&n==1)redis.expire(k,2,TimeUnit.MINUTES);if(n!=null&&n>20)throw new IllegalStateException("Webhook rate limit exceeded");}
    private JsonNode post(String url,Object p){ResponseEntity<JsonNode>r=http.postForEntity(url,new HttpEntity<>(p,jsonHeaders()),JsonNode.class);return r.getBody();}
    private HttpHeaders jsonHeaders(){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);return h;}
    private String write(Object o){try{return json.writeValueAsString(o);}catch(Exception e){return"{}";}}
    private Long longValue(Object v){if(v==null)return null;try{return Long.valueOf(v.toString());}catch(Exception e){return null;}}
    private String join(Object v){if(v instanceof Collection)return String.join("|",((Collection<?>)v).stream().map(Object::toString).toList());return null;}
    private String first(String...v){for(String x:v)if(x!=null&&!x.isBlank())return x;return null;}
}
