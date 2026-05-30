package com.thedal.thedal_app.election;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect to check if an election is frozen before allowing write operations
 */
@Aspect
@Component
@Slf4j
public class ElectionFreezeInterceptor {

    @Autowired
    private ElectionRepository electionRepository;

    /**
     * Annotation to mark methods that should be blocked when election is frozen
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CheckElectionNotFrozen {
        /**
         * Parameter index for electionId (default is 0 - first parameter)
         */
        int electionIdParamIndex() default 0;
        
        /**
         * Custom error message (optional)
         */
        String message() default "Cannot perform this operation. Election is frozen.";
    }

    @Around("@annotation(checkElectionNotFrozen)")
    public Object checkFrozenStatus(ProceedingJoinPoint joinPoint, CheckElectionNotFrozen checkElectionNotFrozen) throws Throwable {
        try {
            // Get method arguments
            Object[] args = joinPoint.getArgs();
            
            if (args.length <= checkElectionNotFrozen.electionIdParamIndex()) {
                log.error("Invalid electionIdParamIndex configuration for method: {}", 
                    joinPoint.getSignature().getName());
                return joinPoint.proceed();
            }

            // Extract electionId from parameters
            Object electionIdObj = args[checkElectionNotFrozen.electionIdParamIndex()];
            
            if (electionIdObj == null) {
                log.warn("ElectionId is null in method: {}", joinPoint.getSignature().getName());
                return joinPoint.proceed();
            }

            Long electionId;
            if (electionIdObj instanceof Long) {
                electionId = (Long) electionIdObj;
            } else if (electionIdObj instanceof Integer) {
                electionId = ((Integer) electionIdObj).longValue();
            } else if (electionIdObj instanceof String) {
                electionId = Long.parseLong((String) electionIdObj);
            } else {
                log.warn("Unsupported electionId type: {} in method: {}", 
                    electionIdObj.getClass().getName(), joinPoint.getSignature().getName());
                return joinPoint.proceed();
            }

            // Check if election is frozen
            ElectionEntity election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_NOT_FOUND, HttpStatus.NOT_FOUND));

            if (Boolean.TRUE.equals(election.getIsFrozen())) {
                log.warn("Attempted write operation on frozen election: electionId={}, method={}", 
                    electionId, joinPoint.getSignature().getName());
                throw new ThedalException(ThedalError.ELECTION_FROZEN, HttpStatus.FORBIDDEN);
            }

            // Election is not frozen, proceed with the operation
            return joinPoint.proceed();

        } catch (ThedalException e) {
            throw e;
        } catch (Throwable e) {
            log.error("Error in ElectionFreezeInterceptor: {}", e.getMessage(), e);
            throw e;
        }
    }
}
