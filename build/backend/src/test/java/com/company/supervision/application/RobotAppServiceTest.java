package com.company.supervision.application;

import com.company.supervision.domain.model.messaging.WecomWebhook;
import com.company.supervision.domain.model.messaging.WecomGroup;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.RobotMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomWebhookMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomGroupMapper;
import com.company.supervision.infrastructure.security.SecretCipher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotAppServiceTest {
    @Test
    void selectableRobotsContainOnlyEnabledDecryptableWebhooks() {
        WecomWebhookMapper hooks = mock(WecomWebhookMapper.class);
        SecretCipher cipher = mock(SecretCipher.class);
        WecomWebhook valid = hook(1L, "运营群", "valid", 1);
        WecomWebhook broken = hook(2L, "失效群", "broken", 1);
        when(hooks.selectList(any())).thenReturn(List.of(valid, broken));
        when(cipher.decrypt("valid")).thenReturn("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test");
        when(cipher.decrypt("broken")).thenThrow(new IllegalStateException("cannot decrypt"));

        WecomGroupMapper groups = mock(WecomGroupMapper.class); WecomGroup group = new WecomGroup(); group.setId(10L); group.setGroupName("运营群"); group.setStatus(1); when(groups.selectById(10L)).thenReturn(group); valid.setGroupId(10L); broken.setGroupId(10L);
        RobotAppService service = new RobotAppService(mock(RobotMapper.class), mock(WechatClient.class), hooks, groups, cipher);

        assertThat(service.listSelectableRobots())
                .extracting("id", "groupName")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, "运营群"));
    }

    @Test
    void rejectsDisabledTarget() {
        WecomWebhookMapper hooks = mock(WecomWebhookMapper.class);
        WecomWebhook disabled = hook(3L, "停用群", "cipher", 0);
        when(hooks.selectById(3L)).thenReturn(disabled);
        RobotAppService service = new RobotAppService(mock(RobotMapper.class), mock(WechatClient.class), hooks, mock(WecomGroupMapper.class), mock(SecretCipher.class));

        assertThatThrownBy(() -> service.validateSelectable(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标群");
    }

    private static WecomWebhook hook(Long id, String name, String cipher, int status) {
        WecomWebhook hook = new WecomWebhook();
        hook.setId(id);
        hook.setName(name);
        hook.setWebhookCipher(cipher);
        hook.setStatus(status);
        return hook;
    }
}
