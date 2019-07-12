package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "author", strict = false)
data class AtomAuthor(
    @field:Element(name = "name", required = false)
    @param:Element(name = "name", required = false)
    val name: String? = null
) : Serializable