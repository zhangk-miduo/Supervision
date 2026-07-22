package com.company.supervision.infrastructure.repository.identity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.identity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    @Select("SELECT r.* FROM supervision_role r JOIN supervision_account_role ar ON ar.role_id=r.id WHERE ar.account_id=#{accountId}") List<Role> selectByAccountId(Long accountId);
    @Select("SELECT * FROM supervision_role WHERE code=#{code} LIMIT 1") Role selectByCode(String code);
}