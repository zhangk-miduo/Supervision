package com.company.supervision.infrastructure.repository.identity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface AccountRoleMapper {
    @Insert("INSERT IGNORE INTO supervision_account_role(account_id,role_id) VALUES(#{accountId},#{roleId})") int insert(Long accountId, Long roleId);
    @Delete("DELETE FROM supervision_account_role WHERE account_id=#{accountId}") int deleteByAccountId(Long accountId);
}