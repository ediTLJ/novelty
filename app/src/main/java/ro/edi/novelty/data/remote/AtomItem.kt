package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "entry", strict = false)
data class AtomItem(
    @field:Element(name = "id")
    @param:Element(name = "id")
    val id: String,

    @field:Element(name = "title")
    @param:Element(name = "title")
    val title: String,

    @field:Element(name = "updated")
    @param:Element(name = "updated")
    val updatedDate: String,

    @field:Element(name = "published", required = false)
    @param:Element(name = "published", required = false)
    val pubDate: String? = null,

    @field:Element(name = "summary", required = false)
    @param:Element(name = "summary", required = false)
    val summary: String? = null,

    @field:Element(name = "content", required = false)
    @param:Element(name = "content", required = false)
    val content: String? = null,

    @field:Element(name = "link", required = false)
    @param:Element(name = "link", required = false)
    val link: AtomLink? = null,

    @field:Element(name = "author", required = false)
    @param:Element(name = "author", required = false)
    val author: AtomAuthor? = null
) : Serializable