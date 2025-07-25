/*
 * Copyright (2021) The Delta Lake Project Authors.
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

package org.apache.spark.sql.catalyst.plans.logical

import org.apache.spark.sql.delta.constraints.{AddConstraint => AddDeltaConstraint, DropConstraint => DropDeltaConstraint} // Aliased to avoid conflicts with Spark's AddConstraint/DropConstraint

import org.apache.spark.sql.connector.catalog.TableChange

/**
 * The logical plan of the ALTER TABLE ... ADD CONSTRAINT command.
 */
case class AlterTableAddConstraint(
    table: LogicalPlan, constraintName: String, expr: String) extends AlterTableCommand {
  override def changes: Seq[TableChange] = Seq(AddDeltaConstraint(constraintName, expr))

  protected def withNewChildInternal(newChild: LogicalPlan): LogicalPlan = copy(table = newChild)
}

/**
 * The logical plan of the ALTER TABLE ... DROP CONSTRAINT command.
 */
case class AlterTableDropConstraint(
    table: LogicalPlan, constraintName: String, ifExists: Boolean) extends AlterTableCommand {
  override def changes: Seq[TableChange] = Seq(DropDeltaConstraint(constraintName, ifExists))

  protected def withNewChildInternal(newChild: LogicalPlan): LogicalPlan = copy(table = newChild)
}
