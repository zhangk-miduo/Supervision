package com.company.supervision.infrastructure.repository.identity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.identity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    @Select("SELECT * FROM supervision_account WHERE username=#{username} LIMIT 1") Account selectByUsername(String username);
}