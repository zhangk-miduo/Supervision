package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.domain.model.messaging.WecomGroup;
import com.company.supervision.domain.model.messaging.WecomWebhook;
import com.company.supervision.entity.dto.SelectableRobot;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.RobotMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomGroupMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomWebhookMapper;
import com.company.supervision.infrastructure.security.SecretCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RobotAppService {
    private final RobotMapper robots;
    private final WechatClient client;
    private final WecomWebhookMapper hooks;
    private final WecomGroupMapper groups;
    private final SecretCipher cipher;

    public RobotAppService(RobotMapper robots, WechatClient client, WecomWebhookMapper hooks, WecomGroupMapper groups, SecretCipher cipher) {
        this.robots=robots; this.client=client; this.hooks=hooks; this.groups=groups; this.cipher=cipher;
    }

    @Transactional
    public Long createRobot(String robotId, String legacyName, String groupName, String pushName, String webhookUrl, String template, Integer status, String remark) {
        String push=first(pushName,legacyName,"消息推送");
        WecomGroup group=group(first(groupName,"待补充群名"),remark);
        WechatRobot robot=new WechatRobot();
        robot.setRobotId(first(robotId,"push-"+UUID.randomUUID().toString().replace("-","").substring(0,12)));
        robot.setName(push); robot.setWebhookUrl("******"); robot.setTemplate(template); robots.insert(robot);
        WecomWebhook hook=new WecomWebhook();
        hook.setId(robot.getId()); hook.setGroupId(group.getId()); hook.setName(push); hook.setPushName(push);
        hook.setSystemCode(robot.getRobotId()); hook.setWebhookCipher(cipher.encrypt(webhookUrl)); hook.setStatus(status==null?1:status);
        hooks.insert(hook); return robot.getId();
    }

    @Transactional
    public void updateRobot(Long id, String legacyName, String groupName, String pushName, String webhookUrl, String template, Integer status, String remark) {
        WechatRobot robot=required(id); WecomWebhook hook=hooks.selectById(id);
        if(hook==null) throw new IllegalStateException("Encrypted webhook configuration not found");
        String push=first(pushName,legacyName,hook.getPushName(),robot.getName());
        WecomGroup group=group(first(groupName,groupName(hook.getGroupId()),"待补充群名"),remark);
        robot.setName(push); robot.setWebhookUrl("******"); if(template!=null)robot.setTemplate(template); robots.updateById(robot);
        hook.setGroupId(group.getId()); hook.setName(push); hook.setPushName(push);
        if(status!=null)hook.setStatus(status);
        if(webhookUrl!=null&&!webhookUrl.isBlank()&&!"******".equals(webhookUrl))hook.setWebhookCipher(cipher.encrypt(webhookUrl));
        hooks.updateById(hook);
    }

    @Transactional public void deleteRobot(Long id){required(id);WecomWebhook hook=hooks.selectById(id);if(hook!=null){hook.setStatus(0);hooks.updateById(hook);}}

    public IPage<WechatRobot> listRobots(String name,int page,int size){
        LambdaQueryWrapper<WechatRobot>w=new LambdaQueryWrapper<>();if(name!=null&&!name.isBlank())w.like(WechatRobot::getName,name);
        IPage<WechatRobot>result=robots.selectPage(new Page<>(Math.max(1,page),Math.max(1,size)),w.orderByDesc(WechatRobot::getId));
        result.getRecords().forEach(this::decorate);return result;
    }

    public List<SelectableRobot> listSelectableRobots(){
        List<SelectableRobot>out=new ArrayList<>();
        for(WecomWebhook hook:hooks.selectList(new LambdaQueryWrapper<WecomWebhook>().eq(WecomWebhook::getStatus,1).isNotNull(WecomWebhook::getWebhookCipher).orderByAsc(WecomWebhook::getPushName))){
            WecomGroup group=groups.selectById(hook.getGroupId());
            if(group!=null&&Objects.equals(group.getStatus(),1)&&isDecryptable(hook)){
                String push=first(hook.getPushName(),hook.getName(),"消息推送");
                out.add(new SelectableRobot(hook.getId(),group.getId(),group.getGroupName(),push,group.getGroupName()+" · "+push));
            }
        }
        return out;
    }

    public void validateSelectable(Long id){WecomWebhook hook=id==null?null:hooks.selectById(id);WecomGroup group=hook==null?null:groups.selectById(hook.getGroupId());if(hook==null||group==null||!Objects.equals(hook.getStatus(),1)||!Objects.equals(group.getStatus(),1)||!isDecryptable(hook))throw new IllegalArgumentException("目标群不存在、已停用或配置无效，请刷新后重新选择");}
    public void validateSelectable(Collection<Long>ids){if(ids==null||ids.isEmpty())throw new IllegalArgumentException("请至少选择一个目标群");ids.forEach(this::validateSelectable);}
    public WecomWebhook webhook(Long id){validateSelectable(id);return hooks.selectById(id);}
    public WecomGroup webhookGroup(Long id){WecomWebhook h=webhook(id);return groups.selectById(h.getGroupId());}

    public WechatRobot getRobot(Long id){WechatRobot r=required(id);decorate(r);return r;}
    public String testRobot(Long id){WechatRobot robot=required(id);WecomWebhook hook=webhook(id);String result=client.send(cipher.decrypt(hook.getWebhookCipher()),"markdown","**Supervision connectivity test**\nPush: "+first(hook.getPushName(),robot.getName()),List.of(),false);hook.setLastTestedAt(LocalDateTime.now());hooks.updateById(hook);return result;}

    private void decorate(WechatRobot r){r.setWebhookUrl("******");WecomWebhook h=hooks.selectById(r.getId());if(h!=null){r.setGroupId(h.getGroupId());r.setGroupName(groupName(h.getGroupId()));r.setPushName(first(h.getPushName(),h.getName(),r.getName()));r.setPushStatus(h.getStatus());r.setLastTestedAt(h.getLastTestedAt());}}
    private WecomGroup group(String name,String remark){WecomGroup g=groups.byName("default",name);if(g==null){g=new WecomGroup();g.setTenantKey("default");g.setGroupName(name);g.setStatus(1);g.setRemark(remark);groups.insert(g);}else if(remark!=null&&!remark.isBlank()){g.setRemark(remark);groups.updateById(g);}return g;}
    private String groupName(Long id){WecomGroup g=id==null?null:groups.selectById(id);return g==null?null:g.getGroupName();}
    private boolean isDecryptable(WecomWebhook h){try{String u=cipher.decrypt(h.getWebhookCipher());return u!=null&&u.startsWith("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?")&&u.contains("key=");}catch(RuntimeException e){return false;}}
    private WechatRobot required(Long id){WechatRobot r=robots.selectById(id);if(r==null)throw new IllegalArgumentException("Robot not found: "+id);return r;}
    private static String first(String... values){for(String v:values)if(v!=null&&!v.isBlank())return v;return null;}
}
