package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.application.identity.CreatorViewService;
import com.company.supervision.api.ResourceNotFoundException;
import com.company.supervision.domain.model.AutomationTask;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.domain.model.messaging.WecomGroup;
import com.company.supervision.domain.model.messaging.WecomWebhook;
import com.company.supervision.entity.dto.RobotRequest;
import com.company.supervision.entity.dto.RobotView;
import com.company.supervision.entity.dto.SelectableRobot;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.RobotMapper;
import com.company.supervision.infrastructure.repository.TaskMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomGroupMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomWebhookMapper;
import com.company.supervision.infrastructure.security.DataScope;
import com.company.supervision.infrastructure.security.SecretCipher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final CreatorViewService creators;
    private final TaskMapper tasks;
    private final ObjectMapper json;

    public RobotAppService(RobotMapper robots, WechatClient client, WecomWebhookMapper hooks,
                           WecomGroupMapper groups, SecretCipher cipher, CreatorViewService creators,
                           TaskMapper tasks, ObjectMapper json) {
        this.robots=robots; this.client=client; this.hooks=hooks; this.groups=groups; this.cipher=cipher;
        this.creators=creators; this.tasks=tasks; this.json=json;
    }

    @Transactional
    public Long createRobot(RobotRequest req, DataScope scope) {
        String push=first(req.getPushName(),req.getName(),"消息推送");
        WecomGroup group=group(scope.accountId(),first(req.getGroupName(),"待补充群名"),req.getRemark());
        WechatRobot robot=new WechatRobot();
        robot.setRobotId(first(req.getRobotId(),"push-"+UUID.randomUUID().toString().replace("-","").substring(0,12)));
        robot.setName(push); robot.setWebhookUrl("******"); robot.setTemplate(req.getTemplate()); robots.insert(robot);
        WecomWebhook hook=new WecomWebhook();
        hook.setId(robot.getId()); hook.setGroupId(group.getId()); hook.setOwnerAccountId(scope.accountId());
        hook.setIsPublic(Boolean.TRUE.equals(req.getIsPublic())?1:0);
        hook.setName(push); hook.setPushName(push); hook.setSystemCode(robot.getRobotId());
        hook.setWebhookCipher(cipher.encrypt(req.getWebhookUrl())); hook.setStatus(req.getStatus()==null?1:req.getStatus());
        hooks.insert(hook); return robot.getId();
    }

    @Transactional
    public void updateRobot(Long id, RobotRequest req, DataScope scope) {
        WechatRobot robot=requiredRobot(id); WecomWebhook hook=requiredOwnedHook(id,scope);
        String push=first(req.getPushName(),req.getName(),hook.getPushName(),robot.getName());
        WecomGroup group=group(scope.accountId(),first(req.getGroupName(),groupName(hook.getGroupId()),"待补充群名"),req.getRemark());
        robot.setName(push); robot.setWebhookUrl("******"); if(req.getTemplate()!=null)robot.setTemplate(req.getTemplate()); robots.updateById(robot);
        hook.setGroupId(group.getId()); hook.setName(push); hook.setPushName(push);
        if(req.getStatus()!=null)hook.setStatus(req.getStatus());
        if(req.getIsPublic()!=null)hook.setIsPublic(req.getIsPublic()?1:0);
        if(req.getWebhookUrl()!=null&&!req.getWebhookUrl().isBlank()&&!"******".equals(req.getWebhookUrl()))hook.setWebhookCipher(cipher.encrypt(req.getWebhookUrl()));
        hooks.updateById(hook);
    }

    @Transactional
    public void deleteRobot(Long id, DataScope scope) {
        requiredRobot(id); WecomWebhook hook=requiredOwnedHook(id,scope); hook.setStatus(0); hooks.updateById(hook);
    }

    public IPage<RobotView> listRobots(String name, String view, Long creatorAccountId, int page, int size, DataScope scope) {
        String selected=view==null||view.isBlank()?"owned":view;
        if("all".equals(selected)&&!scope.admin())throw new IllegalArgumentException("Administrator view required");
        if(creatorAccountId!=null&&!scope.admin())throw new IllegalArgumentException("Creator filter requires administrator permission");
        LambdaQueryWrapper<WechatRobot>w=new LambdaQueryWrapper<>();
        if(name!=null&&!name.isBlank())w.like(WechatRobot::getName,name);
        if("owned".equals(selected))w.inSql(WechatRobot::getId,"SELECT id FROM supervision_wecom_webhook WHERE owner_account_id="+scope.accountId());
        else if("public".equals(selected))w.inSql(WechatRobot::getId,"SELECT id FROM supervision_wecom_webhook WHERE is_public=1 AND owner_account_id IS NOT NULL AND owner_account_id<>"+scope.accountId());
        else if(!"all".equals(selected))throw new IllegalArgumentException("Unknown robot view: "+selected);
        if(creatorAccountId!=null)w.inSql(WechatRobot::getId,"SELECT id FROM supervision_wecom_webhook WHERE owner_account_id="+creatorAccountId);
        IPage<WechatRobot>raw=robots.selectPage(new Page<>(Math.max(1,page),Math.max(1,size)),w.orderByDesc(WechatRobot::getId));
        Page<RobotView>out=new Page<>(raw.getCurrent(),raw.getSize(),raw.getTotal());
        out.setRecords(raw.getRecords().stream().map(r->view(r,scope)).toList()); return out;
    }

    public List<SelectableRobot> listSelectableRobots(DataScope scope) {
        LambdaQueryWrapper<WecomWebhook>w=new LambdaQueryWrapper<WecomWebhook>()
                .eq(WecomWebhook::getStatus,1).isNotNull(WecomWebhook::getWebhookCipher)
                .and(x->x.eq(WecomWebhook::getOwnerAccountId,scope.accountId()).or().eq(WecomWebhook::getIsPublic,1))
                .orderByAsc(WecomWebhook::getPushName);
        List<SelectableRobot>out=new ArrayList<>();
        for(WecomWebhook hook:hooks.selectList(w)){
            WecomGroup group=groups.selectById(hook.getGroupId());
            if(group!=null&&Objects.equals(group.getStatus(),1)&&isDecryptable(hook)){
                String push=first(hook.getPushName(),hook.getName(),"消息推送");
                Account account=creators.find(hook.getOwnerAccountId());
                out.add(new SelectableRobot(hook.getId(),group.getId(),group.getGroupName(),push,
                        group.getGroupName()+" · "+push,Objects.equals(hook.getIsPublic(),1),hook.getOwnerAccountId(),
                        account==null?null:account.getUsername(),account==null?null:account.getDisplayName()));
            }
        }
        return out;
    }

    public void validateSelectable(Collection<Long>ids, DataScope scope) {
        if(ids==null||ids.isEmpty())throw new IllegalArgumentException("请至少选择一个目标群");
        ids.forEach(id->requireUsable(id,scope.accountId()));
    }

    public WecomWebhook requireUsable(Long id, Long callerAccountId) {
        WecomWebhook hook=id==null?null:hooks.selectById(id);
        if(hook==null)throw new IllegalArgumentException("TARGET_UNAVAILABLE");
        if(!Objects.equals(hook.getOwnerAccountId(),callerAccountId)&&!Objects.equals(hook.getIsPublic(),1))
            throw new IllegalArgumentException("ROBOT_SHARE_REVOKED");
        WecomGroup group=groups.selectById(hook.getGroupId());
        if(!Objects.equals(hook.getStatus(),1)||group==null||!Objects.equals(group.getStatus(),1))
            throw new IllegalArgumentException("ROBOT_DISABLED");
        if(!isDecryptable(hook))throw new IllegalArgumentException("TARGET_UNAVAILABLE");
        return hook;
    }

    public RobotView getRobot(Long id, DataScope scope) {
        WechatRobot robot=requiredRobot(id); WecomWebhook hook=hooks.selectById(id);
        boolean publicSummary=hook!=null&&Objects.equals(hook.getIsPublic(),1);
        if(hook==null||(!scope.canRead(hook.getOwnerAccountId())&&!publicSummary))throw notFound();
        return view(robot,scope);
    }

    public String testRobot(Long id, DataScope scope) {
        WechatRobot robot=requiredRobot(id); WecomWebhook hook=requiredOwnedHook(id,scope);
        requireUsable(id,scope.accountId());
        String result=client.send(cipher.decrypt(hook.getWebhookCipher()),"markdown","**Supervision connectivity test**\nPush: "+first(hook.getPushName(),robot.getName()),List.of(),false);
        hook.setLastTestedAt(LocalDateTime.now());hooks.updateById(hook);return result;
    }

    public Map<String,Object> usageImpact(Long id, DataScope scope) {
        requiredOwnedHook(id,scope); long count=tasks.selectList(new LambdaQueryWrapper<AutomationTask>().eq(AutomationTask::getStatus,1))
                .stream().filter(t->!Objects.equals(t.getOwnerAccountId(),scope.accountId())).filter(t->references(t,id)).count();
        return Map.of("externalTaskCount",count);
    }

    private RobotView view(WechatRobot robot, DataScope scope) {
        WecomWebhook hook=hooks.selectById(robot.getId()); if(hook==null)throw notFound();
        WecomGroup group=groups.selectById(hook.getGroupId()); RobotView out=new RobotView();
        out.setId(robot.getId());out.setRobotId(robot.getRobotId());out.setGroupId(hook.getGroupId());
        out.setGroupName(group==null?null:group.getGroupName());out.setPushName(first(hook.getPushName(),hook.getName(),robot.getName()));
        out.setTemplate(scope.owns(hook.getOwnerAccountId())||scope.admin()?robot.getTemplate():null);
        out.setWebhookUrl(scope.owns(hook.getOwnerAccountId())||scope.admin()?"******":null);
        out.setPushStatus(hook.getStatus());out.setIsPublic(Objects.equals(hook.getIsPublic(),1));
        out.setCanUse(scope.owns(hook.getOwnerAccountId())||Objects.equals(hook.getIsPublic(),1));
        out.setLastTestedAt(scope.owns(hook.getOwnerAccountId())||scope.admin()?hook.getLastTestedAt():null);
        out.setCreatedAt(robot.getCreatedAt());creators.fill(out,hook.getOwnerAccountId(),scope);return out;
    }

    private WecomWebhook requiredOwnedHook(Long id,DataScope scope){WecomWebhook h=hooks.selectById(id);if(h==null||!scope.owns(h.getOwnerAccountId()))throw notFound();return h;}
    private WechatRobot requiredRobot(Long id){WechatRobot r=robots.selectById(id);if(r==null)throw notFound();return r;}
    private ResourceNotFoundException notFound(){return new ResourceNotFoundException("Robot not found");}
    private WecomGroup group(Long owner,String name,String remark){WecomGroup g=groups.byName("default",owner,name);if(g==null){g=new WecomGroup();g.setTenantKey("default");g.setOwnerAccountId(owner);g.setGroupName(name);g.setStatus(1);g.setRemark(remark);groups.insert(g);}else if(remark!=null&&!remark.isBlank()){g.setRemark(remark);groups.updateById(g);}return g;}
    private String groupName(Long id){WecomGroup g=id==null?null:groups.selectById(id);return g==null?null:g.getGroupName();}
    private boolean isDecryptable(WecomWebhook h){try{String u=cipher.decrypt(h.getWebhookCipher());return u!=null&&u.startsWith("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?")&&u.contains("key=");}catch(RuntimeException e){return false;}}
    private boolean references(AutomationTask task,Long id){try{JsonNode root=json.readTree(task.getMessageDefinition());JsonNode many=root.path("webhookIds");if(many.isArray())for(JsonNode n:many)if(n.asLong()==id)return true;return root.path("webhookId").asLong(Long.MIN_VALUE)==id;}catch(Exception e){return false;}}
    private static String first(String... values){for(String v:values)if(v!=null&&!v.isBlank())return v;return null;}
}
