package com.cuzz.rookiepostbox.repository.mybatis.spi;

import com.cuzz.rookiepostbox.repository.mybatis.row.MailEventRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailEventMapper {

    @Insert("""
            INSERT INTO mail_events (
                package_id,
                actor_uuid,
                actor_name_snapshot,
                actor_type,
                event_type,
                payload_json,
                created_at
            ) VALUES (
                #{packageId},
                CAST(#{actorUuid} AS UUID),
                #{actorNameSnapshot},
                #{actorType},
                #{eventType},
                #{payloadJson},
                COALESCE(#{createdAt}, CURRENT_TIMESTAMP)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MailEventRow record);

    @Select("""
            SELECT *
            FROM mail_events
            WHERE package_id = #{packageId}
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    @Results(id = "mailEventRowMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "packageId", column = "package_id"),
            @Result(property = "actorUuid", column = "actor_uuid"),
            @Result(property = "actorNameSnapshot", column = "actor_name_snapshot"),
            @Result(property = "actorType", column = "actor_type"),
            @Result(property = "eventType", column = "event_type"),
            @Result(property = "payloadJson", column = "payload_json"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<MailEventRow> selectByPackageId(@Param("packageId") long packageId,
                                         @Param("limit") int limit);
}
