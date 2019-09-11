package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "item", strict = false)
@Namespace(prefix = "dc", reference = "http://purl.org/dc/elements/1.1/")
data class RssItem(
    @field:Element(name = "title", required = false)
    @param:Element(name = "title", required = false)
    val title: String? = null,

    @field:Element(name = "link", required = false)
    @param:Element(name = "link", required = false)
    val link: String? = null,

    @field:Element(name = "description", required = false)
    @param:Element(name = "description", required = false)
    val description: String? = null,

    @field:Element(name = "creator", required = false)
    @param:Element(name = "creator", required = false)
    val author: String? = null,

    @field:Element(name = "id", required = false)
    @param:Element(name = "id", required = false)
    val id: String? = null,

    @field:Element(name = "pubDate", required = false)
    @param:Element(name = "pubDate", required = false)
    val pubDate: String? = null
) : Serializable