//package com.thedal.thedal_app.voterApiTest;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.slf4j.Logger;
//
//import com.thedal.thedal_app.voterAPI.VoterEntity;
//import com.thedal.thedal_app.voterAPI.VoterRepo;
//import com.thedal.thedal_app.voterAPI.VoterServiceImpl;
//import com.thedal.thedal_app.voterAPI.dto.VoterDTO;
//import com.thedal.thedal_app.voterAPI.dto.VoterLocationDTO;
//
//public class VoterServiceImplTest {
//	
//	@InjectMocks
//    private VoterServiceImpl voterServiceImpl;
//
//    @Mock
//    private VoterRepo voterRepository;
//    
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void testSaveVoter() {
//        VoterEntity voter = new VoterEntity();
//        when(voterRepository.save(voter)).thenReturn(voter);
//
//        VoterEntity savedVoter = voterServiceImpl.saveVoter(voter);
//        assertNotNull(savedVoter);
//        verify(voterRepository, times(1)).save(voter);
//    }
//
//    @Test
//    public void testUpdateVoter() {
//        String voterId = "voter1";
//        VoterEntity existingVoter = new VoterEntity();
//        VoterEntity voterDetails = new VoterEntity();
//        VoterEntity updatedVoter = new VoterEntity();
//        when(voterRepository.findById(voterId)).thenReturn(Optional.of(existingVoter));
//        when(voterRepository.save(existingVoter)).thenReturn(updatedVoter);
//
//        VoterEntity result = voterServiceImpl.updateVoter(voterId, voterDetails);
//        assertEquals(updatedVoter, result);
//        verify(voterRepository, times(1)).findById(voterId);
//        verify(voterRepository, times(1)).save(existingVoter);
//    }
//
//    @Test
//    public void testDeleteVoter() {
//        String voterId = "voter1";
//        doNothing().when(voterRepository).deleteById(voterId);
//        when(voterRepository.findById(voterId)).thenReturn(Optional.of(new VoterEntity()));
//
//        String result = voterServiceImpl.deleteVoter(voterId);
//        assertEquals("Voter deleted successfully", result);
//        verify(voterRepository, times(1)).deleteById(voterId);
//    }
//
//    @Test
//    public void testGetAllVoterLocations() {
//        VoterEntity voter1 = new VoterEntity();
//        voter1.setVoterId("voter1");
//        voter1.setLatitude(37.7749);
//        voter1.setLongitude(-122.4194);
//        VoterEntity voter2 = new VoterEntity();
//        voter2.setVoterId("voter2");
//        voter2.setLatitude(34.0522);
//        voter2.setLongitude(-118.2437);
//
//        when(voterRepository.findAll()).thenReturn(List.of(voter1, voter2));
//
//        List<VoterLocationDTO> locations = voterServiceImpl.getAllVoterLocations();
//        assertEquals(2, locations.size());
//        assertEquals("voter1", locations.get(0).getVoterId());
//        assertEquals(37.7749, locations.get(0).getLatitude());
//        assertEquals(-122.4194, locations.get(0).getLongitude());
//    }
//    
//    @Test
//    public void testGetVoterById() {
//       
//        String voterId = "VOTER123";
//        VoterEntity mockVoterEntity = new VoterEntity(
//                voterId,
//                "EPIC123456",
//                "John",
//                "Doe",
//                LocalDate.of(1985, 5, 20),
//                "Male",
//                null, 
//                "1234567890",
//                "john.doe@example.com",
//                37.7749,
//                -122.4194,
//                "Available",
//                "Independent",
//                "Christian",
//                "Caste1",
//                "THIRD123",
//                "http://photo.url",
//                "Remarks"
//        );
//        when(voterRepository.findById(voterId)).thenReturn(Optional.of(mockVoterEntity));
//       
//        VoterDTO voterDTO = voterServiceImpl.getVoterById(voterId);
//      
//        assertNotNull(voterDTO);
//        assertEquals(voterId, voterDTO.getVoterId());
//        assertEquals("John", voterDTO.getFirstName());
//        assertEquals("Doe", voterDTO.getLastName());
//        verify(voterRepository, times(1)).findById(voterId);
//    }    
//
//    @Test
//    public void testFindVoterByEpicNumber() {
//        
//        String epicNumber = "EPIC123456";
//        VoterEntity voterEntity = new VoterEntity();
//        voterEntity.setEpicNumber(epicNumber);
//        voterEntity.setFirstName("John");
//        voterEntity.setLastName("Doe");
//       
//        when(voterRepository.findByEpicNumber(epicNumber)).thenReturn(voterEntity);
//       
//        VoterEntity result = voterServiceImpl.findVoterByEpicNumber(epicNumber);
//        
//        assertNotNull(result);
//        assertEquals(epicNumber, result.getEpicNumber());
//        assertEquals("John", result.getFirstName());
//        assertEquals("Doe", result.getLastName());
//       
//        verify(voterRepository, times(1)).findByEpicNumber(epicNumber);
//    }
//
//
//    
//}
