/*
 * Copyright (c) 2011 Miles Sabin 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.{ EclipseKeys, EclipseCreateSrc }

object ShapelessBuild extends Build {
  
  override lazy val settings = super.settings :+ (
    EclipseKeys.skipParents := false
  )

  lazy val shapeless = Project(
    id = "shapeless", 
    base = file("."),
    aggregate = Seq(shapelessCore, shapelessExamples, allExampleRun),
    settings = commonSettings ++ Seq(
      moduleName := "shapeless-root",
        
      (unmanagedSourceDirectories in Compile) := Nil,
      (unmanagedSourceDirectories in Test) := Nil,
      
      publish := (),
      publishLocal := ()
    )
  )

  lazy val shapelessCore =
    Project(
      id = "shapeless-core", 
      base = file("core"),
      settings = commonSettings ++ Publishing.settings ++ Seq(
        moduleName := "shapeless",
        
        managedSourceDirectories in Test := Nil,
        
        EclipseKeys.createSrc := EclipseCreateSrc.Default+EclipseCreateSrc.Managed,
        
        libraryDependencies <++= scalaVersion { sv =>
          Seq(
            "org.scala-lang" % "scala-compiler" % sv,
            "com.novocode" % "junit-interface" % "0.7" % "test"
        )},
        
        (sourceGenerators in Compile) <+= (sourceManaged in Compile) map Boilerplate.gen,

        mappings in (Compile, packageSrc) <++=
          (sourceManaged in Compile, managedSources in Compile) map { (base, srcs) =>
            (srcs x (Path.relativeTo(base) | Path.flat))
          },
          
        mappings in (Compile, packageSrc) <++=
          (mappings in (Compile, packageSrc) in LocalProject("shapeless-examples"))
      )
    )

  lazy val shapelessExamples = Project(
    id = "shapeless-examples",
    base = file("examples"),
    dependencies = Seq(shapelessCore),
    
    settings = commonSettings ++ Seq(
      libraryDependencies <++= scalaVersion { sv =>
        Seq(
          "org.scala-lang" % "scala-compiler" % sv,
          "com.novocode" % "junit-interface" % "0.7" % "test"
      )},

      publish := (),
      publishLocal := ()
    )
  )

  lazy val genAllMainRun = TaskKey[File]("gen-all-main-run")

  lazy val allExampleRun = Project(
    id = "all-example-run",
    base = file("all-example-run"),
    dependencies = Seq(shapelessExamples),
    settings = commonSettings ++ Seq(
      genAllMainRun <<= (sourceManaged in Compile, discoveredMainClasses in Compile in shapelessExamples).map{ 
        (dir,mainClasses) =>
        val ignoreClasses = Seq("NewtypeExampes","LinearAlgebraExamples").map("shapeless.examples." + _)
        val src =
        """package shapeless.example
          |
          |object AllMainRun extends App{
          |  %s
          |}
          |""".stripMargin.format(mainClasses.filterNot(ignoreClasses.contains).map{_ + ".main(Array())"}.mkString("\n"))

        val generated:File = dir / "shapeless" / "AllMainRun.scala"
        IO.write(generated, src)
        generated
      },
      sourceGenerators in Compile <+= genAllMainRun.map(Seq(_))
    )
  )
  
  def commonSettings = Defaults.defaultSettings ++
    Seq(
      organization        := "com.chuusai",
      version             := "1.2.5-SNAPSHOT",
      scalaVersion        := "2.10.1",

      (unmanagedSourceDirectories in Compile) <<= (scalaSource in Compile)(Seq(_)),
      (unmanagedSourceDirectories in Test) <<= (scalaSource in Test)(Seq(_)),

      scalacOptions       := Seq(
        "-feature",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-deprecation",
        "-unchecked"),

      resolvers           ++= Seq(
        Classpaths.typesafeSnapshots,
        "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
      )
    )
}
