package com.cuzz.rookiepostbox.repository.mybatis.spi;

import com.cuzz.rookiepostbox.repository.mybatis.row.PostBoxRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostBoxMapper {

    @Insert("""
            INSERT INTO post_boxes (owner_uuid, owner_name_cache)
            SELECT CAST(#{ownerUuid} AS UUID), #{ownerNameCache}
            WHERE NOT EXISTS (
                SELECT 1
                FROM post_boxes
                WHERE owner_uuid = CAST(#{ownerUuid} AS UUID)
            )
            """)
    int insertIfAbsent(@Param("ownerUuid") String ownerUuid,
                       @Param("ownerNameCache") String ownerNameCache);

    @Select("""
            SELECT owner_uuid, owner_name_cache, mailbox_enabled, unread_count, last_opened_at, created_at, updated_at
            FROM post_boxes
            WHERE owner_uuid = CAST(#{ownerUuid} AS UUID)
            """)
    @Results(id = "postBoxRowMap", value = {
            @Result(property = "ownerUuid", column = "owner_uuid", id = true),
            @Result(property = "ownerNameCache", column = "owner_name_cache"),
            @Result(property = "mailboxEnabled", column = "mailbox_enabled"),
            @Result(property = "unreadCount", column = "unread_count"),
            @Result(property = "lastOpenedAt", column = "last_opened_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    PostBoxRow selectByOwnerUuid(@Param("ownerUuid") String ownerUuid);
}
