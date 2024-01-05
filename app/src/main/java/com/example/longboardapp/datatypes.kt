import com.beust.klaxon.Converter
import com.beust.klaxon.Json
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlin.math.PI
import kotlin.reflect.KClass

// CONSTANTS
const val REVS_PER_EREV = 16f/36f/7f
const val MILES_PER_REV = PI.toFloat()*0.09f/1000f*0.621371f
const val MPH_PER_RPM = MILES_PER_REV*60f

// MESSAGE
data class Message (
    @TypeFor(field = "fields", adapter = DataTypeAdapter::class)
    val id: MessageID,
    val fields: Data,
) {
    constructor(id: MessageID) : this(id, Data())
}

enum class MessageID(val id: String, val data: KClass<out Data>?) {
    COMM_FW_VERSION("COMM_FW_VERSION", null),
    COMM_GET_VALUES("COMM_GET_VALUES", Values::class),
    COMM_SET_DUTY("COMM_SET_DUTY", null),
    COMM_SET_CURRENT("COMM_SET_CURRENT", null),
    COMM_SET_CURRENT_BRAKE("COMM_SET_CURRENT_BRAKE", null),
    COMM_SET_RPM("COMM_SET_RPM", null),
    COMM_SET_POS("COMM_SET_POS", null),
    COMM_SET_DETECT("COMM_SET_DETECT", null),
    COMM_SET_SERVO_POS("COMM_SET_SERVO_POS", null),
    COMM_ROTOR_POSITION("COMM_ROTOR_POSITION", null),
    COMM_ALIVE("COMM_ALIVE", null),
    GET_TARGET("GET_TARGET", Target::class),
    SET_TARGET("SET_TARGET", Target::class),
    SET_SETTINGS("SET_SETTINGS", Settings::class),
    GET_SETTINGS("GET_SETTINGS", Settings::class),
}

val MessageIDConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
            = cls == MessageID::class.java

    override fun toJson(value: Any): String
            = "\"${(value as MessageID).id}\""

    override fun fromJson(jv: JsonValue): MessageID {
        jv.string?.let { id -> return MessageID.entries.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown type: $id") }
        throw KlaxonException("Could not parse MessageID Object: $jv")
    }
}

// DATA
open class Data

class DataTypeAdapter: TypeAdapter<Data> {
    override fun classFor(id: Any): KClass<out Data> {
        return MessageID.entries.firstOrNull { it.id == id as String }?.data ?: throw IllegalArgumentException("Unknown type: ${id as String}")
    }
}

// CHARGE
enum class ChargeFormat{
    VOLTS,
    PERCENT,
}

data class Charge(val volts: Float){
    constructor(charge: Float, format: ChargeFormat = ChargeFormat.VOLTS) : this(when(format) {
        ChargeFormat.VOLTS -> charge
        ChargeFormat.PERCENT -> charge*6f + 44.4f
    })
    val percent get() = (volts-44.4f)/6f
}

val ChargeConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
            = cls == Charge::class.java

    override fun toJson(value: Any): String
            = "${(value as Charge).volts}"

    override fun fromJson(jv: JsonValue): Charge {
        jv.int?.let { return Charge(it.toFloat()) }
        jv.float?.let { return Charge(it) }
        jv.double?.let { return Charge(it.toFloat()) }
        throw KlaxonException("Could not parse MotorSpeed Object: $jv")
    }
}

// DISTANCE
enum class DistanceFormat{
    EREVS,
    REVS,
    MILES,
}

data class Distance(val erevs: Int){
    constructor(distance: Float, format: DistanceFormat = DistanceFormat.EREVS) : this(when(format) {
        DistanceFormat.EREVS -> distance.toInt()
        DistanceFormat.REVS -> (distance/REVS_PER_EREV).toInt()
        DistanceFormat.MILES -> (distance/MILES_PER_REV/REVS_PER_EREV).toInt()
    })
    val miles get() = MILES_PER_REV*REVS_PER_EREV*erevs
    val revs get() = REVS_PER_EREV*erevs
}

val DistanceConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
            = cls == Distance::class.java

    override fun toJson(value: Any): String
            = "${(value as Distance).erevs}"

    override fun fromJson(jv: JsonValue): Distance {
        jv.int?.let { return Distance(it) }
        jv.float?.let { return Distance(it) }
        jv.double?.let { return Distance(it.toFloat()) }
        throw KlaxonException("Could not parse MotorSpeed Object: $jv")
    }
}

// SPEED
enum class SpeedFormat{
    ERPM,
    RPM,
    MPH,
}

data class Speed(val erpm: Int){
    constructor(speed: Float, format: SpeedFormat = SpeedFormat.ERPM) : this(when(format) {
        SpeedFormat.ERPM -> speed.toInt()
        SpeedFormat.RPM -> (speed/REVS_PER_EREV).toInt()
        SpeedFormat.MPH -> (speed/MPH_PER_RPM/REVS_PER_EREV).toInt()
    })
    val mph get() = MPH_PER_RPM*REVS_PER_EREV*erpm
    val rpm get() = REVS_PER_EREV*erpm
}

val SpeedConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
            = cls == Speed::class.java

    override fun toJson(value: Any): String
            = "${(value as Speed).erpm}"

    override fun fromJson(jv: JsonValue): Speed {
        jv.int?.let { return Speed(it) }
        jv.float?.let { return Speed(it) }
        jv.double?.let { return Speed(it.toFloat()) }
        throw KlaxonException("Could not parse MotorSpeed Object: $jv")
    }
}

// DATA CHILDREN
data class Target(
    @Json(name = "rpm")
    val erpm: Speed?
) : Data()

data class Values(
    val temp_fet: Float? = 0f,
    val temp_motor: Float? = 0f,
    val avg_motor_current: Float? = 0f,
    val avg_input_current: Float? = 0f,
    val avg_id: Float? = 0f,
    val avg_iq: Float? = 0f,
    val duty_cycle_now: Float? = 0f,
    @Json(name = "rpm")
    val speed: Speed? = Speed(0),
    @Json(name = "v_in")
    val charge: Charge? = Charge(0f),
    val amp_hours: Float? = 0f,
    val amp_hours_charged: Float? = 0f,
    val watt_hours: Float? = 0f,
    val watt_hours_charged: Float? = 0f,
    @Json(name = "tachometer")
    val distance: Distance? = Distance(0),
    @Json(name = "tachometer_abs")
    val distance_abs: Distance? = Distance(0),
    val mc_fault_code: Int? = 0,
    val pid_pos_now: Float? = 0f,
    val app_controller_id: Int? = 0,
    val time_ms: Float? = 0f,
) : Data()

data class Settings(
    var ACC_RPM_PER_SECOND: Float? = 0f,
    var DEC_RPM_PER_SECOND: Float? = 0f,
) : Data()

// PARSING TOOLS
val KlaxonInstance = Klaxon()
    .converter(SpeedConverter)
    .converter(DistanceConverter)
    .converter(ChargeConverter)
    .converter(MessageIDConverter)

fun parseMessage(message: String): Message {
    return KlaxonInstance.parse<Message>(message) ?: throw IllegalArgumentException("Could not parse $message as Message")
}

fun encodeMessage(message: Message): String {
    return KlaxonInstance.toJsonString(message)
}