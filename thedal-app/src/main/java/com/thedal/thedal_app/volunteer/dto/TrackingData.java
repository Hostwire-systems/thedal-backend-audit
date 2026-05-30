//package com.thedal.thedal_app.volunteer.dto;
//
//import java.util.List;
//
//import com.thedal.thedal_app.volunteer.ActivityEntity;
//
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//public class TrackingData {
//    private String volunteerId;
//    private List<ActivityDto> activities;
//	
//    public void setActivities(List<ActivityEntity> fetchVolunteerActivities) {
//        // Convert ActivityEntity to ActivityDto
//        if (fetchVolunteerActivities != null) {
//            this.activities = fetchVolunteerActivities.stream()
//                .map(activity -> {
//                    ActivityDto dto = new ActivityDto();
//                    //dto.setDate(activity.getDate());
//                    dto.setDate(activity.getDate());
//                    dto.setBooth(activity.getBooth());
//                    dto.setVotersInteracted(activity.getVotersInteracted());
//                    dto.setRemarks(activity.getRemarks());
////                    dto.setLatitude(activity.getLatitude());
////                    dto.setLongitude(activity.getLongitude());
//                    dto.setLocation(activity.getLocation());
//                    dto.setRoute(activity.getRoute());
//                    return dto;
//                })
//                .toList();
//        } else {
//            this.activities = null;
//        }
//    }
//        
//    
//}
