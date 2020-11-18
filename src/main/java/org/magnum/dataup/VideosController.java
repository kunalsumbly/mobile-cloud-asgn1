/*
 * 
 * Copyright 2014 Jules White
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
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideosController {
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();
	
	private VideoFileManager videoMgr;
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	
	
	@PostConstruct
	public void init() throws IOException {
		videoMgr = VideoFileManager.get();
	}
	
	/**
	 * Get the video list from server
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/video", method=RequestMethod.GET)
	 public @ResponseBody Collection<Video> getVideoList() throws IOException {
		 return videos.values();
	 }
	
	/** 
	 * POST method at the url "/video" to add a video to the video collection 
	 * @param video
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value="/video", method=RequestMethod.POST)
	 public @ResponseBody Video addVideo( @RequestBody Video video) {
		return save(video);
	 }
	
	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id")  long id, 
			@RequestParam("data") MultipartFile videoData, 
			HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
		InputStream inputStream = videoData.getInputStream();
			// find the video by id 
			Video video = videos.get(id);
			if (video == null) {
				response.setStatus(HttpStatus.NOT_FOUND.value()); 
				return null;
			} else {
				videoMgr.saveVideoData(video, inputStream);
				inputStream.close();
			}
			VideoStatus status = new VideoStatus(VideoState.READY);
			return status;
		
	}
	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET)
	public @ResponseBody void getData(@PathVariable("id")  long id , HttpServletResponse response) throws IOException {
		if (videos.get(id) == null || !videoMgr.hasVideoData(videos.get(id))) {
			 response.setStatus(HttpStatus.NOT_FOUND.value()); 
		} else {
			videoMgr.copyVideoData(videos.get(id), response.getOutputStream());
			response.flushBuffer();
		}
	 }
	
	/**
	 * Get the data URL for a videoId
	 * @param videoId
	 * @return
	 */
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

	/**
	 * Get the base URL for the local server
	 * @return
	 */
 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}
	
 	/**
 	 * Save the video to the server collection
 	 * @param entity
 	 * @return
 	 */
 	public Video save(Video entity) {
		checkAndSetId(entity);
		String dataUrl = getDataUrl(entity.getId());
		entity.setDataUrl(dataUrl);
		videos.put(entity.getId(), entity);
		return entity;
	}

 	/**
 	 * Generate the video id for the newly added video
 	 * 
 	 * @param entity
 	 */
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
	
}
