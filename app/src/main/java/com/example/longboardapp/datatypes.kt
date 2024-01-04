import com.beust.klaxon.Klaxon
import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlin.reflect.KClass

data class Message (
    @TypeFor(field = "fields", adapter = DataTypeAdapter::class)
    val id: String,
    val fields: Data,
)

open class Data

data class Target(val target: Int?) : Data()

data class Values(val temp_fet: Float? = 0f,
                  val temp_motor: Float? = 0f,
                  val avg_motor_current: Float? = 0f,
                  val avg_input_current: Float? = 0f,
                  val avg_id: Float? = 0f,
                  val avg_iq: Float? = 0f,
                  val duty_cycle_now: Float? = 0f,
                  val rpm: Float? = 0f,
                  val v_in: Float? = 0f,
                  val amp_hours: Float? = 0f,
                  val amp_hours_charged: Float? = 0f,
                  val watt_hours: Float? = 0f,
                  val watt_hours_charged: Float? = 0f,
                  val tachometer: Float? = 0f,
                  val tachometer_abs: Float? = 0f,
                  val mc_fault_code: Int? = 0,
                  val pid_pos_now: Float? = 0f,
                  val app_controller_id: Int? = 0,
                  val time_ms: Float? = 0f,
) : Data()



class DataTypeAdapter: TypeAdapter<Data> {
    override fun classFor(id: Any): KClass<out Data> = when(id as String) {
        "COMM_GET_VALUES" -> Values::class
        "getTarget" -> Target::class
        "setTarget" -> Target::class
        else -> throw IllegalArgumentException("Unknown type: $id")
    }
}

fun parseMessage(message: String): Message? {
    return Klaxon().parse<Message>(message)
}

fun encodeMessage(message: Message): String? {
    return Klaxon().toJsonString(message)
}