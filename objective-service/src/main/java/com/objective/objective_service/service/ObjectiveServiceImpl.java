package com.objective.objective_service.service;

import com.objective.objective_service.entity.KeyResult;
import com.objective.objective_service.entity.Objective;
import com.objective.objective_service.exception.ObjectiveNotFoundException;
import com.objective.objective_service.repository.ObjectiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class ObjectiveServiceImpl implements ObjectiveService {

    private final ObjectiveRepository objectiveRepository;
    private static final Logger LOGGER = Logger.getLogger(ObjectiveServiceImpl.class.getName());
    private static final String KEYRESULT_SERVICE_URL = "http://localhost:8082/api/keyresult/";

    @Autowired
    public ObjectiveServiceImpl(ObjectiveRepository objectiveRepository){
        this.objectiveRepository = objectiveRepository;
    }

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public List<Objective> getAllObjective() {
        LOGGER.info("Fetching all objectives from the database...");
        return objectiveRepository.findAll();
    }

    @Override
    public Objective getObjective(Long id) {
        LOGGER.info("Fetching objective with ID: " + id);

        // Fetch Objective from the database
        Optional<Objective> objectiveOpt = objectiveRepository.findById(id);
        if (!objectiveOpt.isPresent()) {
            throw new ObjectiveNotFoundException("Objective not found with ID: " + id);
        }

        Objective objective = objectiveOpt.get();

        // Fetch related KeyResults from KeyResult Microservice
        String url = KEYRESULT_SERVICE_URL + "objective/" + id;
        LOGGER.info("Fetching key results from: " + url);

        try {
            List<KeyResult> keyResults = Arrays.asList(
                    restTemplate.getForObject(url, KeyResult[].class)
            );
            // Set KeyResults to the Objective
            objective.setKeyResult(keyResults);
        } catch (RestClientException e) {
            LOGGER.severe("Failed to fetch key results for Objective ID: " + id + " - " + e.getMessage());
            throw new RuntimeException("Failed to fetch key results from the microservice.");
        }
        return objective;
    }

    @Override
    public Objective createObjective(Objective obj) {
        LOGGER.info("Creating a new objective...");
        return objectiveRepository.save(obj);
    }

    @Override
    public Objective updateObjective(Objective objectiveToUpdate, Long objectiveId) {
        LOGGER.info("Updating objective with ID: "+ objectiveId);

        // Check if the objective exists
        Objective existingObjective = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> new ObjectiveNotFoundException("Objective with ID " + objectiveId + " not found."));

        // Update fields of the existing objective
        existingObjective.setObjectiveName(objectiveToUpdate.getObjectiveName());
        existingObjective.setObjectiveDescription(objectiveToUpdate.getObjectiveDescription());
        existingObjective.setMappedProject(objectiveToUpdate.getMappedProject());
        existingObjective.setAssignedTo(objectiveToUpdate.getAssignedTo());
        existingObjective.setKeyResultIds(objectiveToUpdate.getKeyResultIds());
        existingObjective.setKeyResult(objectiveToUpdate.getKeyResult());
        existingObjective.setObjectiveStartDate(objectiveToUpdate.getObjectiveStartDate());
        existingObjective.setObjectiveDueDate(objectiveToUpdate.getObjectiveDueDate());
        existingObjective.setObjectiveStatus(objectiveToUpdate.getObjectiveStatus());
        existingObjective.setObjectiveIsActive(objectiveToUpdate.isObjectiveIsActive());

        // Save the updated objective
        Objective updatedObjective = objectiveRepository.save(existingObjective);
        LOGGER.info("Objective with ID: " + objectiveId + "updated successfully.");

        return updatedObjective;
    }

    
    @Override
    public void removeObjective(Long objectiveId) {
        LOGGER.info("Removing objective with ID: " + objectiveId);

        Objective objectiveToDelete = getObjective(objectiveId);
        objectiveRepository.deleteById(objectiveId);
    }
}