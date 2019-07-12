package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "feed", strict = false)
data class BaseAtomFeed(
    @field:Element(name = "title")
    @param:Element(name = "title")
    val title: String,

    @field:ElementList(name = "entry", inline = true, required = false)
    @param:ElementList(name = "entry", inline = true, required = false)
    val items: List<AtomItem>? = ArrayList()
) : Serializable