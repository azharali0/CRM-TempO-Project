package com.crm.config;

import com.crm.model.entity.User;
import com.crm.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * AOP aspect that automatically captures audit log entries for create/update/delete
 * operations on service methods. Runs @Async so audit logging never blocks the main operation.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.CustomerService.createCustomer(..))",
            returning = "result")
    public void auditCustomerCreate(JoinPoint joinPoint, Object result) {
        logAudit("CUSTOMER", extractId(result), "CREATE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.CustomerService.updateCustomer(..))",
            returning = "result")
    public void auditCustomerUpdate(JoinPoint joinPoint, Object result) {
        logAudit("CUSTOMER", extractId(result), "UPDATE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.CustomerService.deleteCustomer(..))")
    public void auditCustomerDelete(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof UUID id) {
            logAudit("CUSTOMER", id, "DELETE");
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.LeadService.createLead(..))",
            returning = "result")
    public void auditLeadCreate(JoinPoint joinPoint, Object result) {
        logAudit("LEAD", extractId(result), "CREATE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.LeadService.updateLead(..))",
            returning = "result")
    public void auditLeadUpdate(JoinPoint joinPoint, Object result) {
        logAudit("LEAD", extractId(result), "UPDATE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.LeadService.changeStage(..))",
            returning = "result")
    public void auditLeadStageChange(JoinPoint joinPoint, Object result) {
        logAudit("LEAD", extractId(result), "STAGE_CHANGE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.TaskService.createTask(..))",
            returning = "result")
    public void auditTaskCreate(JoinPoint joinPoint, Object result) {
        logAudit("TASK", extractId(result), "CREATE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.TaskService.updateTask(..))",
            returning = "result")
    public void auditTaskUpdate(JoinPoint joinPoint, Object result) {
        logAudit("TASK", extractId(result), "UPDATE");
    }

    @AfterReturning(
            pointcut = "execution(* com.crm.service.TaskService.completeTask(..))",
            returning = "result")
    public void auditTaskComplete(JoinPoint joinPoint, Object result) {
        logAudit("TASK", extractId(result), "UPDATE");
    }

    private void logAudit(String entityType, UUID entityId, String action) {
        try {
            User currentUser = getCurrentUser();
            String ipAddress = getClientIp();
            if (currentUser != null && entityId != null) {
                auditLogService.logAction(entityType, entityId, action,
                        null, null, null, currentUser, ipAddress);
            }
        } catch (Exception e) {
            // Audit logging should never break the main flow
            log.warn("Failed to create audit log entry: {}", e.getMessage());
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP", e);
        }
        return null;
    }

    private UUID extractId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            return (UUID) result.getClass().getMethod("getId").invoke(result);
        } catch (Exception e) {
            return null;
        }
    }
}
