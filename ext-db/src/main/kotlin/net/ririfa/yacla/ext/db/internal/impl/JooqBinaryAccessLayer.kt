@file:Suppress("SqlNoDataSourceInspection", "SqlSourceToSinkFlow")

package net.ririfa.yacla.ext.db.internal.impl

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import net.ririfa.yacla.ext.db.internal.DBAccessLayer
import org.jooq.DSLContext
import org.jooq.Table
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class JooqBinaryAccessLayer(
    private val ctx: DSLContext,
    private val table: Table<*>
) : DBAccessLayer {

    private val kryo = Kryo().apply {
        isRegistrationRequired = false // Allow flexible types without pre-registration
    }

    override fun <T : Any> load(clazz: Class<T>, key: String): T? {
        val record = ctx.selectFrom(table)
            .where(table.field("config_key", String::class.java)?.eq(key))
            .and(table.field("config_type", String::class.java)?.eq(clazz.name))
            .fetchOne() ?: return null

        val bytes = record.get(table.field("config_blob", ByteArray::class.java))
        return bytes?.let {
            val input = Input(ByteArrayInputStream(it))
            kryo.readObject(input, clazz)
        }
    }

    override fun <T : Any> save(clazz: Class<T>, key: String, config: T): Boolean {
        val outStream = ByteArrayOutputStream()
        val output = Output(outStream)
        kryo.writeObject(output, config)
        output.close()
        val bytes = outStream.toByteArray()

        ctx.insertInto(table)
            .set(table.field("config_key", String::class.java), key)
            .set(table.field("config_type", String::class.java), clazz.name)
            .set(table.field("config_blob", ByteArray::class.java), bytes)
            .onDuplicateKeyUpdate()
            .set(table.field("config_blob", ByteArray::class.java), bytes)
            .execute()
        return true
    }
}
