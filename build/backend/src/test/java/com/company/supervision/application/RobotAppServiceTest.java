package com.company.supervision.application;

import com.company.supervision.api.ResourceNotFoundException;
import com.company.supervision.application.identity.CreatorViewService;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.domain.model.messaging.*;
import com.company.supervision.entity.dto.*;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.*;
import com.company.supervision.infrastructure.repository.messaging.*;
import com.company.supervision.infrastructure.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RobotAppServiceTest {
    @Test void selectableIncludesOwnedAndPublicButExcludesBroken(){
        WecomWebhookMapper hooks=mock(WecomWebhookMapper.class);SecretCipher cipher=mock(SecretCipher.class);
        WecomWebhook owned=hook(1L,1L,0,"valid",1),shared=hook(2L,2L,1,"valid2",1),broken=hook(3L,1L,0,"broken",1);
        when(hooks.selectList(any())).thenReturn(List.of(owned,shared,broken));when(cipher.decrypt("valid")).thenReturn(url());when(cipher.decrypt("valid2")).thenReturn(url());when(cipher.decrypt("broken")).thenThrow(new IllegalStateException("cannot decrypt"));
        WecomGroupMapper groups=groups(owned,shared,broken);RobotAppService service=service(mock(RobotMapper.class),hooks,groups,cipher);
        assertThat(service.listSelectableRobots(new DataScope(1L,false))).extracting("id").containsExactly(1L,2L);
    }

    @Test void privateRobotCannotBeUsedByAnotherAccount(){
        WecomWebhookMapper hooks=mock(WecomWebhookMapper.class);WecomWebhook privateHook=hook(3L,2L,0,"cipher",1);when(hooks.selectById(3L)).thenReturn(privateHook);
        assertThatThrownBy(()->service(mock(RobotMapper.class),hooks,groups(privateHook),mock(SecretCipher.class)).requireUsable(3L,1L)).isInstanceOf(IllegalArgumentException.class).hasMessage("ROBOT_SHARE_REVOKED");
    }

    @Test void administratorCannotModifyAnotherAccountsRobot(){
        RobotMapper robots=mock(RobotMapper.class);WechatRobot robot=new WechatRobot();robot.setId(3L);when(robots.selectById(3L)).thenReturn(robot);
        WecomWebhookMapper hooks=mock(WecomWebhookMapper.class);when(hooks.selectById(3L)).thenReturn(hook(3L,2L,1,"cipher",1));
        assertThatThrownBy(()->service(robots,hooks,mock(WecomGroupMapper.class),mock(SecretCipher.class)).deleteRobot(3L,new DataScope(1L,true))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void newlyCreatedRobotIsPrivateAndOwnedBySession(){
        RobotMapper robots=mock(RobotMapper.class);doAnswer(i->{WechatRobot r=i.getArgument(0);r.setId(5L);return 1;}).when(robots).insert(any());
        WecomGroupMapper groups=mock(WecomGroupMapper.class);doAnswer(i->{WecomGroup g=i.getArgument(0);g.setId(15L);return 1;}).when(groups).insert(any());
        WecomWebhookMapper hooks=mock(WecomWebhookMapper.class);SecretCipher cipher=mock(SecretCipher.class);when(cipher.encrypt(any())).thenReturn("encrypted");
        RobotRequest request=new RobotRequest();request.setGroupName("运营群");request.setPushName("通知");request.setWebhookUrl(url());
        service(robots,hooks,groups,cipher).createRobot(request,new DataScope(7L,false));
        verify(hooks).insert(argThat(h->Long.valueOf(7L).equals(h.getOwnerAccountId())&&Integer.valueOf(0).equals(h.getIsPublic())));
    }

    @Test void publicSummaryDoesNotExposeWebhookOrEditableConfiguration(){
        RobotMapper robots=mock(RobotMapper.class);WechatRobot robot=new WechatRobot();robot.setId(3L);robot.setTemplate("secret-template");when(robots.selectById(3L)).thenReturn(robot);
        WecomWebhookMapper hooks=mock(WecomWebhookMapper.class);WecomWebhook hook=hook(3L,2L,1,"cipher",1);when(hooks.selectById(3L)).thenReturn(hook);
        RobotView view=service(robots,hooks,groups(hook),mock(SecretCipher.class)).getRobot(3L,new DataScope(1L,false));
        assertThat(view.getWebhookUrl()).isNull();assertThat(view.getTemplate()).isNull();assertThat(view.isCanEdit()).isFalse();assertThat(view.getIsPublic()).isTrue();
    }

    private RobotAppService service(RobotMapper robots,WecomWebhookMapper hooks,WecomGroupMapper groups,SecretCipher cipher){return new RobotAppService(robots,mock(WechatClient.class),hooks,groups,cipher,mock(CreatorViewService.class),mock(TaskMapper.class),new ObjectMapper());}
    private WecomGroupMapper groups(WecomWebhook...hooks){WecomGroupMapper mapper=mock(WecomGroupMapper.class);for(WecomWebhook hook:hooks){WecomGroup group=new WecomGroup();group.setId(hook.getGroupId());group.setStatus(1);group.setGroupName("群"+hook.getId());when(mapper.selectById(hook.getGroupId())).thenReturn(group);}return mapper;}
    private WecomWebhook hook(Long id,Long owner,int pub,String cipher,int status){WecomWebhook h=new WecomWebhook();h.setId(id);h.setGroupId(100L+id);h.setOwnerAccountId(owner);h.setIsPublic(pub);h.setName("推送"+id);h.setWebhookCipher(cipher);h.setStatus(status);return h;}
    private String url(){return "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test";}
}
