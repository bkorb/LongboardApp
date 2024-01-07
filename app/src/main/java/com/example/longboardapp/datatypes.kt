import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.math.PI

// CONSTANTS
const val REVS_PER_EREV = 16f/36f/7f
const val MILES_PER_REV = PI.toFloat()*0.09f/1000f*0.621371f
const val MPH_PER_RPM = MILES_PER_REV*60f

// MESSAGE
@Serializable
sealed class Message {
    abstract val fields: Data?
}

@Serializable
@SerialName("COMM_FW_VERSION")
class FirmwareVersion(): Message() {
    override val fields = null
}

@Serializable
@SerialName("COMM_GET_VALUES")
class GetValues(override val fields: Values? = null): Message()

@Serializable
@SerialName("GET_TARGET")
class GetTarget(override val fields: Target? = null): Message()

@Serializable
@SerialName("SET_TARGET")
class SetTarget(override val fields: Target): Message() {
    constructor(target: Number): this(Target(target))
    constructor(target: Speed): this(Target(target))
}

@Serializable
@SerialName("GET_SETTINGS")
class GetSettings(override val fields: Settings? = null): Message()

@Serializable
@SerialName("SET_SETTINGS")
class SetSettings(override val fields: Settings): Message()


/*@Serializable
enum class MessageID(val id: String, val data: KClass<out Any>?) {
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
    SET_SETTINGS("SET_SETTINGS", MySettings::class),
    GET_SETTINGS("GET_SETTINGS", MySettings::class),
}*/


// DATA
interface Data


// CHARGE
enum class ChargeFormat{
    VOLTS,
    PERCENT,
}

@Serializable
@JvmInline
value class Charge(val volts: Float){
    constructor(charge: Float, format: ChargeFormat = ChargeFormat.VOLTS) : this(when(format) {
        ChargeFormat.VOLTS -> charge
        ChargeFormat.PERCENT -> charge*6f + 44.4f
    })
    val percent get() = (volts-44.4f)/6f
}

// DISTANCE
enum class DistanceFormat{
    EREVS,
    REVS,
    MILES,
}

@Serializable
@JvmInline
value class Distance(val erevs: Float){
    constructor(distance: Float, format: DistanceFormat = DistanceFormat.EREVS) : this(when(format) {
        DistanceFormat.EREVS -> distance
        DistanceFormat.REVS -> (distance/REVS_PER_EREV)
        DistanceFormat.MILES -> (distance/MILES_PER_REV/REVS_PER_EREV)
    })
    val miles get() = MILES_PER_REV*REVS_PER_EREV*erevs
    val revs get() = REVS_PER_EREV*erevs
}

// SPEED
enum class SpeedFormat{
    ERPM,
    RPM,
    MPH,
}

@Serializable
@JvmInline
value class Speed(val erpm: Float){
    constructor(speed: Float, format: SpeedFormat = SpeedFormat.ERPM) : this(when(format) {
        SpeedFormat.ERPM -> speed
        SpeedFormat.RPM -> (speed/REVS_PER_EREV)
        SpeedFormat.MPH -> (speed/MPH_PER_RPM/REVS_PER_EREV)
    })
    val mph get() = MPH_PER_RPM*REVS_PER_EREV*erpm
    val rpm get() = REVS_PER_EREV*erpm
}

@Serializable(with = SettingsSerializer::class)
class Settings(): Data, MutableMap<String, Float> by HashMap() {
    constructor(map: Map<String, Float>) : this() {
        this.putAll(map)
    }
}

class SettingsSerializer : KSerializer<Settings> {
    private val delegateSerializer = MapSerializer(String.serializer(), Float.serializer())
    override val descriptor = SerialDescriptor("Settings", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Settings) {
        val data = value as MutableMap<String, Float>
        encoder.encodeSerializableValue(delegateSerializer, data)
    }

    override fun deserialize(decoder: Decoder): Settings {
        val map = decoder.decodeSerializableValue(delegateSerializer).toMutableMap()
        return Settings(map)
    }
}

// DATA CHILDREN
@Serializable
data class Target(
    @SerialName("rpm")
    val erpm: Speed
) : Data {
    constructor(erpm: Number): this(Speed(erpm.toFloat()))
}

@Serializable
data class Values(
    val temp_fet: Float = 0f,
    val temp_motor: Float = 0f,
    val avg_motor_current: Float = 0f,
    val avg_input_current: Float = 0f,
    val avg_id: Float = 0f,
    val avg_iq: Float = 0f,
    val duty_cycle_now: Float = 0f,
    @SerialName("rpm")
    val speed: Speed = Speed(0f),
    @SerialName("v_in")
    val charge: Charge = Charge(0f),
    val amp_hours: Float = 0f,
    val amp_hours_charged: Float = 0f,
    val watt_hours: Float = 0f,
    val watt_hours_charged: Float = 0f,
    @SerialName("tachometer")
    val distance: Distance = Distance(0f),
    @SerialName("tachometer_abs")
    val distance_abs: Distance = Distance(0f),
    val mc_fault_code: Int = 0,
    val pid_pos_now: Float = 0f,
    val app_controller_id: Int = 0,
    val time_ms: Float = 0f,
) : Data

// PARSING TOOLS

val format = Json {
    classDiscriminator = "id"
    encodeDefaults = true
}
fun parseMessage(message: String): Message {
    return format.decodeFromString<Message>(message)
}

fun encodeMessage(message: Message): String {
    return format.encodeToString(message)
}