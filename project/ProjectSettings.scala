import sbt._
import sbt.nio.Keys._
import sbt.Keys._
import sbtbuildinfo.BuildInfoKeys.buildInfoOptions
import sbtbuildinfo.BuildInfoPlugin.autoImport.{BuildInfoKey, buildInfoKeys}
import sbtbuildinfo.{BuildInfoOption, BuildInfoPlugin}
import sbtghactions.GitHubActionsPlugin.autoImport._
import sbtghactions.GenerativePlugin.autoImport._
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import RefPredicate._
import Ref._
import UseRef._

import scala.sys.process._
import scala.util.Try

/** Allows customizations of build.sbt syntax.
  */
object ProjectSettings {

  // TODO since Git 2.22 we could use the following command instead: git branch --show-current
  private val currentBranch: Option[String] = Try(
    Process(s"git rev-parse --abbrev-ref HEAD").lineStream_!.head
  ).toOption

  private val commitSha: Option[String] = Try(Process(s"git rev-parse --short HEAD").lineStream_!.head).toOption

  private val interfaceVersion: String = ComputeVersion.version match {
    case ComputeVersion.tag(major, minor, _) => s"$major.$minor"
    case _                                   => "0.0"
  }

  // lifts some useful data in BuildInfo instance
  val buildInfoExtra: Seq[BuildInfoKey] = Seq[BuildInfoKey](
    "ciBuildNumber"    -> sys.env.get("BUILD_NUMBER"),
    "commitSha"        -> commitSha,
    "currentBranch"    -> currentBranch,
    "interfaceVersion" -> interfaceVersion
  )

  /** Extention methods for sbt Project instances.
    * @param project
    */
  implicit class ProjectFrom(project: Project) {
    def setupBuildInfo: Project = {
      project
        .enablePlugins(BuildInfoPlugin)
        .settings(buildInfoKeys ++= buildInfoExtra)
        .settings(buildInfoOptions += BuildInfoOption.BuildTime)
        .settings(buildInfoOptions += BuildInfoOption.ToJson)
    }
  }

  val sbtGithubActionsSettings: List[Def.Setting[_]] = List[Def.Setting[_]](
    githubWorkflowPublishTargetBranches := Seq(Equals(Branch("1.0.x")), StartsWith(Tag("v"))),
    githubWorkflowTargetTags            := Seq("v*"),
    githubWorkflowScalaVersions         := Seq("2.13.10"),
    githubWorkflowBuildPreamble         := Seq(
      WorkflowStep.Use(
        Public("actions", "setup-node", "v3"),
        name = Some("Install node 16"),
        params = Map("node-version" -> "16")
      ),
      WorkflowStep.Run(
        List("npm install -g @openapitools/openapi-generator-cli"),
        name = Some("Installing openapi-generator-cli")
      )
    ),
    githubOwner                         := "pagopa",
    githubRepository                    := "interop-commons",
    githubWorkflowPublishPostamble      := Seq(
      WorkflowStep.Use(
        Public("aws-actions", "configure-aws-credentials", "v1"),
        name = Some("Configure AWS Credentials"),
        params = Map(
          "aws-region"        -> "eu-central-1",
          "role-to-assume"    -> "arn:aws:iam::505630707203:role/interop-github-ecr-dev",
          "role-session-name" -> "thisShouldBeTheRepoName-${{ github.workflow }}-${{ github.run_id }}"
        )
      ),
      WorkflowStep.Use(
        Public("aws-actions", "amazon-ecr-login", "v1"),
        name = Some("Login to Amazon ECR"),
        id = Some("login-ecr")
      ),
      WorkflowStep.Sbt(List("docker:publish"), name = Some("Build, tag, and push image to Amazon ECR"))
    )
  )
}
