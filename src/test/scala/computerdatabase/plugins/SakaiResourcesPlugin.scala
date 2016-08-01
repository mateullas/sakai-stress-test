package plugins

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SakaiResourcesPlugin extends SakaiSimulationPlugin {

	def name(): String = { "resources-upload" }
	
	def description(): String = { "Upload and remove a file" }
	
	def toolid(): String = { "sakai-resources" }
 
	// Define an infinite feeder which calculates random file names
	val randomFileNames = Iterator.continually(
	  // Random number will be accessible in session under variable "randomFileName"
	  Map("randomFileName" -> ("img" + util.Random.nextInt(Integer.MAX_VALUE) + ".jpg"))
	)
 
  	def getSimulationChain =
  		group("ResourcesUpload") {
	  		exec(http("Resources")
				.get("${tool._2}")
				.headers(headers)
				.check(status.is(successStatus))
				.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
				.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
				.check(css("form[name='showForm']","action").optional.saveAs("resources_upload_url"))
				.check(css("input[name='collectionId']","value").optional.saveAs("resources_upload_collection_id"))
				.check(css("input[name='sakai_csrf_token']","value").optional.saveAs("resources_upload_csrf_token"))
				.check(css("a[onclick*='fileUpload:create']","onclick").optional.saveAs("resources_upload")))
			.pause(pauseMin,pauseMax)
			.doIf("${resources_upload.exists()}") {
				exec(http("ResourcesUploadForm")
					.post("${resources_upload_url}")
					.headers(headers)
					.formParam("source","0")
					.formParam("collectionId","${resources_upload_collection_id}")
					.formParam("navRoot","")
					.formParam("criteria","title")
					.formParam("sakai_action","doDispatchAction")
					.formParam("rt_action","org.sakaiproject.content.types.fileUpload:create")
					.formParam("selectedItemId","${resources_upload_collection_id}")
					.formParam("sakai_csrf_token","${resources_upload_csrf_token}")
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("input[id='requestStateId']","value").saveAs("resources_upload_request_state_id"))
					.check(css("input[id='pipe-init-id']","value").saveAs("resources_upload_pipe_init_id"))
					.check(css("form[name='dropzone-form']","action").saveAs("resources_upload_dzurl")))
				.feed(randomFileNames)
				.pause(pauseMin,pauseMax)
				.exec(http("ResourcesUploadFile")
					.post("${resources_upload_dzurl}")
					.headers(headers)
					.formParam("sakai_action","doPost")
					.formParam("flow","save")
					.formParam("fullPath","undefined")
					.formParam("hidden","false")
					.formParam("sakai_csrf_token","${resources_upload_csrf_token}")
					.bodyPart(RawFileBodyPart("file", "global2.jpg").contentType("image/jpeg").fileName("${randomFileName}")).asMultipartForm
					.check(status.is(successStatus)))
				.pause(pauseMin,pauseMax)
				.exec(http("ResourcesFinishUpload")
					.post("${resources_upload_dzurl}")
					.headers(headers)
					.formParam("sakai_action","doFinishUpload")
					.formParam("pipe_init_id","${resources_upload_pipe_init_id}")
					.formParam("requestStateId","${resources_upload_request_state_id}")
					.formParam("sakai_csrf_token","${resources_upload_csrf_token}")
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("a[href*='${randomFileName}']").exists))
				.pause(pauseMin,pauseMax)
				.exec(http("ResourcesMoveToTrash")
					.post("${resources_upload_url}")
					.headers(headers)
					.formParam("source","0")
					.formParam("collectionId","${resources_upload_collection_id}")
					.formParam("navRoot","")
					.formParam("criteria","title")
					.formParam("sakai_action","doDispatchAction")
					.formParam("rt_action","org.sakaiproject.content.types.fileUpload:delete")
					.formParam("selectedItemId","${resources_upload_collection_id}${randomFileName}")
					.formParam("itemHidden","false")
					.formParam("itemCanRevise","true")
					.formParam("sakai_csrf_token","${resources_upload_csrf_token}")
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("input[name='eventSubmit_doFinalizeDelete']").exists)
					.check(css("form[name='deleteFileForm']","action").saveAs("resources_upload_dfurl")))
				.pause(pauseMin,pauseMax)
				.exec(http("ResourcesConfirmDelete")
					.post("${resources_upload_dfurl}")
					.headers(headers)
					.formParam("collectionId","${resources_upload_collection_id}")
					.formParam("eventSubmit_doFinalizeDelete","Remove")
					.formParam("sakai_csrf_token","${resources_upload_csrf_token}")
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("a[href*='doViewTrash']","href").saveAs("resources_upload_vturl"))
					.check(css("a[href*='${randomFileName}']").notExists))
				.pause(pauseMin,pauseMax)
		  		.exec(http("ResourcesGoToTrash")
					.get("${resources_upload_vturl}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("input[id='restore-button']").exists)
					.check(css("input[name='folderId']","value").saveAs("resources_upload_folder_id"))
					.check(css("form[name='restoreForm']","action").saveAs("resources_upload_rfurl")))
				.pause(pauseMin,pauseMax)
				.exec(http("ResourcesTrashDelete")
					.post("${resources_upload_rfurl}")
					.headers(headers)
					.formParam("folderId","${resources_upload_folder_id}")
					.formParam("sakai_action","doRestore")
					.formParam("flow","remove")
					.formParam("selectedMembers","${resources_upload_collection_id}${randomFileName}")
					.formParam("sakai_csrf_token","${resources_upload_csrf_token}")
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("div[id='messageSuccessHolder']").exists))
				.exec(session => { 
					session
						.remove("resources_upload") 
						.remove("resources_upload_url")
						.remove("resources_upload_dzurl")
						.remove("resources_upload_dfurl")
						.remove("resources_upload_vturl")
						.remove("resources_upload_rfurl")
						.remove("resources_upload_collection_id")
						.remove("resources_upload_csrf_token")
						.remove("resources_upload_request_state_id")
						.remove("resources_upload_pipe_init_id")
						.remove("resources_upload_folder_id")
				})
				
			}
		}

}
