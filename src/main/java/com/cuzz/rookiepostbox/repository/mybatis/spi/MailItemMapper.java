package com.cuzz.rookiepostbox.repository.mybatis.spi;

import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailItemMapper {

    @Insert("""
            INSERT INTO mail_items (
                package_id,
                slot_index,
                item_kind,
                material_key,
                display_name,
                amount,
                store_id,
                unique_key,
                base64_item,
                created_at
            ) VALUES (
                #{packageId},
                #{slotIndex},
                #{itemKind},
                #{materialKey},
                #{displayName},
                #{amount},
                #{storeId},
                #{uniqueKey},
                #{base64Item},
                COALESCE(#{createdAt}, CURRENT_TIMESTAMP)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MailItemRecord record);

    @Select("""
            SELECT *
            FROM mail_items
            WHERE package_id = #{packageId}
            ORDER BY slot_index ASC
            """)
    @Results(id = "mailItemRecordMap", value = {
            @Result(property = "id", column = "id", id = true),
            @Result(property = "packageId", column = "package_id"),
            @Result(property = "slotIndex", column = "slot_index"),
            @Result(property = "itemKind", column = "item_kind"),
            @Result(property = "materialKey", column = "material_key"),
            @Result(property = "displayName", column = "display_name"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "storeId", column = "store_id"),
            @Result(property = "uniqueKey", column = "unique_key"),
            @Result(property = "base64Item", column = "base64_item"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<MailItemRecord> selectByPackageId(@Param("packageId") long packageId);

    @Select("""
            <script>
            SELECT *
            FROM mail_items
            WHERE package_id IN
            <foreach collection="packageIds" item="packageId" open="(" separator="," close=")">
                #{packageId}
            </foreach>
            ORDER BY package_id ASC, slot_index ASC
            </script>
            """)
    @ResultMap("mailItemRecordMap")
    List<MailItemRecord> selectByPackageIds(@Param("packageIds") List<Long> packageIds);
}
