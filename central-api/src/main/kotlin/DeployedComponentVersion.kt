import kotlin.String
import kotlin.collections.List
import kotlinx.serialization.Serializable

@Serializable
public data class DeployedComponentVersion(
  public val name: String? = null,
  public val path: String? = null,
  public val errors: List<String>,
)
