/*
 * Copyright (2024) The Delta Lake Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.delta.kernel.internal.actions

import java.lang.{Long => JLong}
import java.util.Optional

import scala.collection.JavaConverters._

import io.delta.kernel.data.Row
import io.delta.kernel.internal.util.{StatsUtils, VectorUtils}
import io.delta.kernel.internal.util.VectorUtils.stringStringMapValue

import org.scalatest.funsuite.AnyFunSuite

class RemoveFileSuite extends AnyFunSuite {
  // For now we use GenerateIcebergCompatActionUtils::createRemoveFileRowWithExtendedFileMetadata
  // because this is the only path we support creating RemoveFile rows currently. In the future when
  // we implement broader support for RemoveFiles we should use the more generic methods to create
  // the test rows
  private def createTestRemoveFileRow(
      path: String,
      deletionTimestamp: Long,
      dataChange: Boolean,
      partitionValues: Map[String, String],
      size: Long,
      stats: Option[String],
      baseRowId: Option[Long] = Option.empty,
      defaultRowCommitVersion: Option[Long] = Option.empty,
      deletionVectorDescriptor: Option[DeletionVectorDescriptor]): Row = {
    def toJavaOptional[T](option: Option[T]): Optional[T] = option match {
      case Some(value) => Optional.of(value)
      case None => Optional.empty()
    }
    GenerateIcebergCompatActionUtils.createRemoveFileRowWithExtendedFileMetadata(
      path,
      deletionTimestamp,
      dataChange,
      stringStringMapValue(partitionValues.asJava),
      size,
      StatsUtils.deserializeFromJson(stats.getOrElse("")),
      null, // physicalSchema
      toJavaOptional(baseRowId.asInstanceOf[Option[JLong]]),
      toJavaOptional(defaultRowCommitVersion.asInstanceOf[Option[JLong]]),
      deletionVectorDescriptor match {
        case Some(dvd) => Optional.of(dvd)
        case None => Optional.empty[DeletionVectorDescriptor]()
      })
  }

  test("getters can read RemoveFile's fields from the backing row") {
    val deletionVectorDescriptor = new DeletionVectorDescriptor(
      "storage",
      "s",
      Optional.of(1),
      25,
      35)
    val removeFileRow = createTestRemoveFileRow(
      path = "test/path",
      deletionTimestamp = 1000L,
      dataChange = true,
      partitionValues = Map("a" -> "1"),
      size = 55555L,
      stats = Option("{\"numRecords\":100}"),
      deletionVectorDescriptor = Some(deletionVectorDescriptor))
    val removeFile = new RemoveFile(removeFileRow)
    assert(removeFile.getPath === "test/path")
    assert(removeFile.getDeletionTimestamp == Optional.of(1000L))
    assert(removeFile.getDataChange)
    assert(removeFile.getExtendedFileMetadata == Optional.of(true))
    assert(removeFile.getPartitionValues.isPresent &&
      VectorUtils.toJavaMap(removeFile.getPartitionValues.get).asScala.equals(Map("a" -> "1")))
    assert(removeFile.getSize == Optional.of(55555L))
    assert(removeFile.getStats.isPresent &&
      removeFile.getStats.get.serializeAsJson(null) == "{\"numRecords\":100}")
    assert(!removeFile.getTags.isPresent)
    assert(removeFile.getDeletionVector.isPresent)
    assert(removeFile.getDeletionVector.get == deletionVectorDescriptor)
    assert(!removeFile.getBaseRowId.isPresent)
    assert(!removeFile.getDefaultRowCommitVersion.isPresent)
  }

  test("getters can read RemoveFile's fields from the backing row with row tracking") {
    val deletionVectorDescriptor = new DeletionVectorDescriptor(
      "storage",
      "s",
      Optional.of(1),
      25,
      35)
    val removeFileRow = createTestRemoveFileRow(
      path = "test/path",
      deletionTimestamp = 1000L,
      dataChange = true,
      partitionValues = Map("a" -> "1"),
      size = 55555L,
      stats = Option("{\"numRecords\":100}"),
      baseRowId = Option(30L),
      defaultRowCommitVersion = Option(40L),
      deletionVectorDescriptor = Some(deletionVectorDescriptor))

    val removeFile = new RemoveFile(removeFileRow)
    assert(removeFile.getPath === "test/path")
    assert(removeFile.getDeletionTimestamp == Optional.of(1000L))
    assert(removeFile.getDataChange)
    assert(removeFile.getExtendedFileMetadata == Optional.of(true))
    assert(removeFile.getPartitionValues.isPresent &&
      VectorUtils.toJavaMap(removeFile.getPartitionValues.get).asScala.equals(Map("a" -> "1")))
    assert(removeFile.getSize == Optional.of(55555L))
    assert(removeFile.getStats.isPresent &&
      removeFile.getStats.get.serializeAsJson(null) == "{\"numRecords\":100}")
    assert(removeFile.getBaseRowId === Optional.of(30L))
    assert(removeFile.getDefaultRowCommitVersion === Optional.of(40L))
    assert(removeFile.getDeletionVector.get == deletionVectorDescriptor)
    assert(!removeFile.getTags.isPresent)
  }
}
