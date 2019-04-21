package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "channel", strict = false)
class RssChannel(
        @field:Element(name = "title")
        @param:Element(name = "title")
        val title: String,

        @field:ElementList(name = "item", inline = true, required = false)
        @param:ElementList(name = "item", inline = true, required = false)
        val items: List<RssItem>? = ArrayList()
) : Serializable