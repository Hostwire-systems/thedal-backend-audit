//package com.thedal.thedal_app.volunteerApiTest;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.thedal.thedal_app.response.BulkAddResponse;
//import com.thedal.thedal_app.response.VolunteerTrackingResponse;
//import com.thedal.thedal_app.volunteerAPI.VolunteerEntity;
//import com.thedal.thedal_app.volunteerAPI.VolunteerRepository;
//import com.thedal.thedal_app.volunteerAPI.VolunteerServiceImpl;
//import com.thedal.thedal_app.volunteerAPI.dto.ActivityDto;
//import com.thedal.thedal_app.volunteerAPI.dto.LocationDto;
//import com.thedal.thedal_app.volunteerAPI.dto.UpdateVolunteerRequest;
//import com.thedal.thedal_app.volunteerAPI.dto.VolunteerLocationDto;
//
//public class VolunteerServiceImplTest {
//	
//	@InjectMocks
//    private VolunteerServiceImpl volunteerService;
//
//    @Mock
//    private VolunteerRepository volunteerRepo;
//
//    @Mock
//    private MultipartFile file;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    
//    @Test
//    void testSaveVolunteer() {
//        VolunteerEntity volunteer = new VolunteerEntity();
//        when(volunteerRepo.save(any(VolunteerEntity.class))).thenReturn(volunteer);
//
//        VolunteerEntity savedVolunteer = volunteerService.saveVolunteer(volunteer);
//        assertNotNull(savedVolunteer);
//        verify(volunteerRepo, times(1)).save(volunteer);
//    }
//    
//    @Test
//    void testGetVolunteerById() throws Exception {
//        VolunteerEntity volunteer = new VolunteerEntity();
//        when(volunteerRepo.findById(anyString())).thenReturn(Optional.of(volunteer));
//
//        Optional<VolunteerEntity> fetchedVolunteer = volunteerService.getVolunteerById("volunteerId");
//        assertTrue(fetchedVolunteer.isPresent());
//    }
//    
//    @Test
//    void testUpdateVolunteer() {
//        VolunteerEntity existingVolunteer = new VolunteerEntity();
//        VolunteerEntity updatedVolunteer = new VolunteerEntity();
//        when(volunteerRepo.findById(anyString())).thenReturn(Optional.of(existingVolunteer));
//        when(volunteerRepo.save(any(VolunteerEntity.class))).thenReturn(updatedVolunteer);
//
//        VolunteerEntity result = volunteerService.updateVolunteer("volunteerId", updatedVolunteer);
//        assertNotNull(result);
//        verify(volunteerRepo, times(1)).save(existingVolunteer);
//    }
//    
//    @Test
//    void testDeleteVolunteer() {
//        when(volunteerRepo.existsById(anyString())).thenReturn(true);
//
//        boolean result = volunteerService.deleteVolunteer("volunteerId");
//        assertTrue(result);
//        verify(volunteerRepo, times(1)).deleteById("volunteerId");
//    }
//    
//    @Test
//    void testGetVolunteerActivities() throws Exception {
//        VolunteerEntity volunteer = new VolunteerEntity();
//        List<ActivityDto> activities = new ArrayList<>();
//        volunteer.setActivities(activities);
//        when(volunteerRepo.findById(anyString())).thenReturn(Optional.of(volunteer));
//
//        List<ActivityDto> result = volunteerService.getVolunteerActivities("volunteerId");
//        assertNotNull(result);
//        assertEquals(activities, result);
//    }
//    
//    @Test
//    void testGetVolunteerTracking() throws Exception {
//        VolunteerEntity volunteer = new VolunteerEntity();
//        VolunteerTrackingResponse response = new VolunteerTrackingResponse();
//        response.setStatus("success");
//        response.setVolunteerId("volunteerId");
//        response.setActivities(volunteer.getActivities());
//        response.setLocation(volunteer.getLocation());
//        response.setRoute("Route C");
//        response.setTotalHoursWorkedToday(5);
//        response.setTotalHoursWorkedWeek(20);
//        when(volunteerRepo.findById(anyString())).thenReturn(Optional.of(volunteer));
//
//        VolunteerTrackingResponse result = volunteerService.getVolunteerTracking("volunteerId");
//        assertNotNull(result);
//        assertEquals("success", result.getStatus());
//    }
//    
//    @Test
//    void testUpdateVolunteerLocation() {
//        VolunteerEntity volunteer = new VolunteerEntity();
//        UpdateVolunteerRequest request = new UpdateVolunteerRequest();
//        request.setLatitude(37.7749);
//        request.setLongitude(-122.4194);
//        request.setRoute("Route A");
//        request.setTotalHoursWorkedToday(5);
//        request.setTotalHoursWorkedWeek(20);
//        when(volunteerRepo.findById(anyString())).thenReturn(Optional.of(volunteer));
//
//        boolean result = volunteerService.updateVolunteerLocation("volunteerId", request);
//        assertTrue(result);
//        verify(volunteerRepo, times(1)).save(volunteer);
//    }
//
//    @Test
//    void testGetAllVolunteersWithLocation() {
//        VolunteerEntity volunteer = new VolunteerEntity();
//        volunteer.setLocation(new LocationDto(37.7749, -122.4194));
//        when(volunteerRepo.findAll()).thenReturn(List.of(volunteer));
//
//        List<VolunteerLocationDto> result = volunteerService.getAllVolunteersWithLocation();
//        assertNotNull(result);
//        assertEquals(1, result.size());
//    }
//    
//    
//    @Test
//    @Disabled("This test is temporarily disabled due to API changes")
//    void testBulkAddVolunteers() throws Exception {
//        String content = "FirstName,LastName,Email,MobileNumber\nJohn,Doe,john.doe@example.com,1234567890";
//        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content.getBytes())));
//        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
//
//        BulkAddResponse response = volunteerService.bulkAddVolunteers(file);
//        assertEquals("success", response.getStatus());
//        assertEquals(1, response.getVolunteersAdded());
//        assertTrue(response.getErrors().isEmpty());
//    }
//
//
//}
