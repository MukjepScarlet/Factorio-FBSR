package moe.mukjep.fbsr.render

import com.demod.fbsr.EntityRendererFactory
import com.demod.fbsr.bs.BSEntity

class EntityRenderingTuple<E : BSEntity>(val entity: E, val factory: EntityRendererFactory<E>)