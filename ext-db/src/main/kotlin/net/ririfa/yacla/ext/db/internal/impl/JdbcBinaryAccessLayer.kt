@file:Suppress("SqlNoDataSourceInspection", "SqlSourceToSinkFlow")

package net.ririfa.yacla.ext.db.internal.impl

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import net.ririfa.yacla.ext.db.internal.DBAccessLayer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.sql.Connection
import javax.sql.DataSource

//TODO
class JdbcBinaryAccessLayer(
    private val dataSource: DataSource,
    private val tableName: String = "yacla_config"
) : DBAccessLayer {

    private val kryo = Kryo().apply {
        isRegistrationRequired = false // Allow flexible types without pre-registration
    }

    override fun <T : Any> load(clazz: Class<T>, key: String): T? {
        val sql = "SELECT config_blob FROM $tableName WHERE config_key = ? AND config_type = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.setString(2, clazz.name)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        val bytes = rs.getBytes("config_blob") ?: return null
                        val input = Input(ByteArrayInputStream(bytes))
                        kryo.readObject(input, clazz)
                    } else null
                }
            }
        }
    }

    override fun <T : Any> save(clazz: Class<T>, key: String, config: T): Boolean {
        val outStream = ByteArrayOutputStream()
        val output = Output(outStream)
        kryo.writeObject(output, config)
        output.close()
        val bytes = outStream.toByteArray()

        dataSource.connection.use { conn ->
            val sql = getUpsertSql(conn, tableName)
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.setString(2, clazz.name)
                stmt.setBytes(3, bytes)
                return stmt.executeUpdate() > 0
            }
        }
    }

    private fun getUpsertSql(conn: Connection, tableName: String): String {
        val product = conn.metaData.databaseProductName.lowercase()
        return when {
            product.contains("postgresql") || product.contains("sqlite") -> """
            INSERT INTO $tableName (config_key, config_type, config_blob)
            VALUES (?, ?, ?)
            ON CONFLICT (config_key, config_type)
            DO UPDATE SET config_blob = EXCLUDED.config_blob
        """.trimIndent()
            product.contains("mysql") || product.contains("mariadb") -> """
            INSERT INTO $tableName (config_key, config_type, config_blob)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE config_blob = VALUES(config_blob)
        """.trimIndent()
            product.contains("h2") -> """
            MERGE INTO $tableName (config_key, config_type, config_blob)
            KEY (config_key, config_type)
            VALUES (?, ?, ?)
        """.trimIndent()
            else -> throw UnsupportedOperationException("Unsupported database: $product")
        }
    }
}
