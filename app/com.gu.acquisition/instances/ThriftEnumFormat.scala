package com.gu.acquisition.instances

import com.twitter.scrooge.ThriftEnum
import play.api.libs.json._

import scala.reflect.ClassTag

private[instances] trait ThriftEnumFormat {

  def thriftEnumReads[A <: ThriftEnum : ClassTag](valueOf: String => Option[A]): Reads[A] = Reads { json =>
    json.validate[String].flatMap { raw =>
      valueOf(raw.filter(_ != '_')).map(JsSuccess(_)).getOrElse {
        JsError(s"Unable to read Thrift enum of type ${reflect.classTag[A].runtimeClass} from $raw")
      }
    }
  }

  def thriftEnumWrites[A <: ThriftEnum]: Writes[A] = Writes { enum =>
    JsString(enum.originalName)
  }
}

