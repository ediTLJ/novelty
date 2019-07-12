package ro.edi.novelty.data.remote

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "link", strict = false)
data class AtomLink(
    @param:Attribute(name = "href", required = false)
    @field:Attribute(name = "href", required = false)
    val href: String? = null
) : Serializable