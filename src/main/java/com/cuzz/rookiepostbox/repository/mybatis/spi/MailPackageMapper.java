package com.cuzz.rookiepostbox.repository.mybatis.spi;

import com.cuzz.rookiepostbox.domain.entity.AdminMailRecord;
import com.cuzz.rookiepostbox.domain.entity.InboxMailRecord;
import com.cuzz.rookiepostbox.repository.mybatis.row.MailPackageRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MailPackageMapper {

    @Insert("""
            INSERT INTO mail_packages (
                mailbox_owner_uuid,
                sender_uuid,
                sender_name_snapshot,
                sender_type,
                message_text,
                read_state,
                lifecycle_state,
                request_id,
                claim_token,
                version,
                source_plugin,
                admin_note,
                created_at,
                expires_at,
                updated_at
            ) VALUES (
                CAST(#{mailboxOwnerUuid} AS UUID),
                CAST(#{senderUuid} AS UUID),
                #{senderNameSnapshot},
                #{senderType},
                #{messageText},
                #{readState},
                #{lifecycleState},
                CAST(#{requestId} AS UUID),
                CAST(#{claimToken} AS UUID),
                #{version},
                #{sourcePlugin},
                #{adminNote},
                COALESCE(#{createdAt}, CURRENT_TIMESTAMP),
                #{expiresAt},
                COALESCE(#{updatedAt}, CURRENT_TIMESTAMP)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MailPackageRow record);

    @Select("""
            SELECT *
            FROM mail_packages
            WHERE id = #{id}
            """)
    @Results(id = "mailPackageRowMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "mailboxOwnerUuid", column = "mailbox_owner_uuid"),
            @Result(property = "senderUuid", column = "sender_uuid"),
            @Result(property = "senderNameSnapshot", column = "sender_name_snapshot"),
            @Result(property = "senderType", column = "sender_type"),
            @Result(property = "messageText", column = "message_text"),
            @Result(property = "readState", column = "read_state"),
            @Result(property = "lifecycleState", column = "lifecycle_state"),
            @Result(property = "requestId", column = "request_id"),
            @Result(property = "claimToken", column = "claim_token"),
            @Result(property = "version", column = "version"),
            @Result(property = "sourcePlugin", column = "source_plugin"),
            @Result(property = "adminNote", column = "admin_note"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "readAt", column = "read_at"),
            @Result(property = "claimStartedAt", column = "claim_started_at"),
            @Result(property = "claimedAt", column = "claimed_at"),
            @Result(property = "expiresAt", column = "expires_at"),
            @Result(property = "deletedAt", column = "deleted_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    MailPackageRow selectById(@Param("id") long id);

    @Select("""
            SELECT *
            FROM mail_packages
            WHERE request_id = CAST(#{requestId} AS UUID)
            """)
    @ResultMap("mailPackageRowMap")
    MailPackageRow selectByRequestId(@Param("requestId") String requestId);

    @Select("""
            SELECT *
            FROM mail_packages
            WHERE deleted_at IS NULL
              AND lifecycle_state IN ('AVAILABLE', 'CLAIM_FAILED')
              AND expires_at IS NOT NULL
              AND expires_at <= CAST(#{now} AS TIMESTAMP WITH TIME ZONE)
            ORDER BY expires_at ASC, id ASC
            LIMIT #{limit}
            """)
    @ResultMap("mailPackageRowMap")
    List<MailPackageRow> selectExpirablePackages(@Param("now") String now,
                                                 @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM mail_packages
            WHERE deleted_at IS NULL
              AND lifecycle_state = 'CLAIMING'
              AND claim_started_at IS NOT NULL
              AND claim_started_at <= CAST(#{cutoff} AS TIMESTAMP WITH TIME ZONE)
            ORDER BY claim_started_at ASC, id ASC
            LIMIT #{limit}
            """)
    @ResultMap("mailPackageRowMap")
    List<MailPackageRow> selectStaleClaimingPackages(@Param("cutoff") String cutoff,
                                                     @Param("limit") int limit);

    @Select("""
            SELECT p.id,
                   p.sender_name_snapshot,
                   p.message_text,
                   p.read_state,
                   p.lifecycle_state,
                   p.created_at,
                   p.expires_at,
                   i.display_name AS first_item_display_name,
                   i.amount AS first_item_amount
            FROM mail_packages p
            LEFT JOIN mail_items i
              ON i.package_id = p.id
             AND i.slot_index = 0
            WHERE p.mailbox_owner_uuid = CAST(#{ownerUuid} AS UUID)
              AND p.deleted_at IS NULL
              AND p.lifecycle_state IN ('AVAILABLE', 'CLAIM_FAILED')
              AND (p.expires_at IS NULL OR p.expires_at > CURRENT_TIMESTAMP)
            ORDER BY p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    @Results(id = "inboxMailRecordMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "senderNameSnapshot", column = "sender_name_snapshot"),
            @Result(property = "messageText", column = "message_text"),
            @Result(property = "readState", column = "read_state"),
            @Result(property = "lifecycleState", column = "lifecycle_state"),
            @Result(property = "firstItemDisplayName", column = "first_item_display_name"),
            @Result(property = "firstItemAmount", column = "first_item_amount"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "expiresAt", column = "expires_at")
    })
    List<InboxMailRecord> selectInbox(@Param("ownerUuid") String ownerUuid,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM mail_packages p
            WHERE p.mailbox_owner_uuid = CAST(#{ownerUuid} AS UUID)
              AND p.deleted_at IS NULL
              AND p.lifecycle_state = 'AVAILABLE'
              AND (p.expires_at IS NULL OR p.expires_at > CURRENT_TIMESTAMP)
            """)
    int countInbox(@Param("ownerUuid") String ownerUuid);

    @Select("""
            SELECT COUNT(*)
            FROM mail_packages p
            WHERE p.mailbox_owner_uuid = CAST(#{ownerUuid} AS UUID)
              AND p.deleted_at IS NULL
              AND p.read_state = 'UNREAD'
              AND p.lifecycle_state = 'AVAILABLE'
              AND (p.expires_at IS NULL OR p.expires_at > CURRENT_TIMESTAMP)
            """)
    int countUnreadInbox(@Param("ownerUuid") String ownerUuid);

    @Select("""
            SELECT p.id,
                   p.sender_name_snapshot,
                   p.message_text,
                   p.read_state,
                   p.lifecycle_state,
                   p.admin_note,
                   p.created_at,
                   i.display_name AS first_item_display_name,
                   i.amount AS first_item_amount
            FROM mail_packages p
            LEFT JOIN mail_items i
              ON i.package_id = p.id
             AND i.slot_index = 0
            WHERE p.mailbox_owner_uuid = CAST(#{ownerUuid} AS UUID)
              AND p.deleted_at IS NULL
            ORDER BY p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    @Results(id = "adminMailRecordMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "senderNameSnapshot", column = "sender_name_snapshot"),
            @Result(property = "messageText", column = "message_text"),
            @Result(property = "readState", column = "read_state"),
            @Result(property = "lifecycleState", column = "lifecycle_state"),
            @Result(property = "firstItemDisplayName", column = "first_item_display_name"),
            @Result(property = "firstItemAmount", column = "first_item_amount"),
            @Result(property = "adminNote", column = "admin_note"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<AdminMailRecord> selectAdminInbox(@Param("ownerUuid") String ownerUuid,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Update("""
            <script>
            UPDATE mail_packages
            SET read_state = 'READ',
                read_at = CAST(#{readAt} AS TIMESTAMP WITH TIME ZONE),
                updated_at = CURRENT_TIMESTAMP
            WHERE mailbox_owner_uuid = CAST(#{ownerUuid} AS UUID)
              AND deleted_at IS NULL
              AND read_state = 'UNREAD'
              AND id IN
              <foreach collection="packageIds" item="packageId" open="(" separator="," close=")">
                #{packageId}
              </foreach>
            </script>
            """)
    int markInboxRead(@Param("ownerUuid") String ownerUuid,
                      @Param("packageIds") List<Long> packageIds,
                      @Param("readAt") String readAt);

    @Update("""
            UPDATE mail_packages
            SET lifecycle_state = 'DELETED',
                deleted_at = CAST(#{deletedAt} AS TIMESTAMP WITH TIME ZONE),
                admin_note = #{adminNote},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{packageId}
              AND deleted_at IS NULL
              AND lifecycle_state IN ('AVAILABLE', 'CLAIM_FAILED', 'EXPIRED')
            """)
    int markDeleted(@Param("packageId") long packageId,
                    @Param("deletedAt") String deletedAt,
                    @Param("adminNote") String adminNote);

    @Update("""
            UPDATE mail_packages
            SET lifecycle_state = #{targetState},
                claim_token = CASE WHEN #{claimToken} IS NULL THEN claim_token ELSE CAST(#{claimToken} AS UUID) END,
                claim_started_at = CASE WHEN #{targetState} = 'CLAIMING' THEN CAST(#{transitionAt} AS TIMESTAMP WITH TIME ZONE) ELSE claim_started_at END,
                claimed_at = CASE WHEN #{targetState} = 'CLAIMED' THEN CAST(#{transitionAt} AS TIMESTAMP WITH TIME ZONE) ELSE claimed_at END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{packageId}
              AND lifecycle_state = #{expectedState}
            """)
    int updateLifecycleState(@Param("packageId") long packageId,
                             @Param("expectedState") String expectedState,
                             @Param("targetState") String targetState,
                             @Param("claimToken") String claimToken,
                             @Param("transitionAt") String transitionAt);
}
