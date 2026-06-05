package com.javaresources.controller;

import com.google.common.collect.Table;
import com.javaresources.model.Employee;
import com.javaresources.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService svc;
    public EmployeeController(EmployeeService svc) { this.svc = svc; }

    @GetMapping
    public List<Map<String,Object>> getAll() {
        return svc.getAll().stream().map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            String name   = (String) body.get("name");
            String email  = (String) body.get("email");
            String dept   = (String) body.get("department");
            String role   = (String) body.get("role");
            double salary = Double.parseDouble(body.get("salary").toString());
            @SuppressWarnings("unchecked")
            List<String> skills = body.containsKey("skills")
                ? (List<String>) body.get("skills") : List.of();
            Employee e = svc.createAndAdd(name, email, dept, role, salary, skills);
            return ResponseEntity.ok(toMap(e));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        boolean deleted = svc.deleteEmployee(id);
        if (deleted) return ResponseEntity.ok(Map.of("message", "Empleado eliminado id=" + id));
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable int id) {
        return svc.findById(id)
            .map(e -> ResponseEntity.ok(toMap(e)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/department/{dept}")
    public List<Map<String,Object>> byDept(@PathVariable String dept) {
        return svc.findByDepartment(dept).stream().map(this::toMap).toList();
    }

    @GetMapping("/top/{n}")
    public List<Map<String,Object>> topEarners(@PathVariable int n) {
        return svc.getTopEarners(n).stream().map(this::toMap).toList();
    }

    @GetMapping("/headcount")
    public Map<String,Integer> headcount() { return svc.getHeadcountByDepartment(); }

    @GetMapping("/cache/stats")
    public Map<String,Object> cacheStats() { return svc.getCacheStats(); }

    @GetMapping("/orgtable")
    public Map<String, Object> orgTable() {
        Table<String, String, List<Employee>> t = svc.getOrgTable();
        Map<String, Object> result = new LinkedHashMap<>();
        for (String dept : t.rowKeySet()) {
            Map<String, Integer> roles = new LinkedHashMap<>();
            for (String role : t.columnKeySet()) {
                List<Employee> cell = t.get(dept, role);
                if (cell != null && !cell.isEmpty()) roles.put(role, cell.size());
            }
            result.put(dept, roles);
        }
        return result;
    }

    @GetMapping("/roles")
    public List<String> validRoles() { return svc.getValidRoles(); }

    private Map<String, Object> toMap(Employee e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("name", e.getName()); m.put("email", e.getEmail());
        m.put("department", e.getDepartment()); m.put("role", e.getRole());
        m.put("salary", e.getSalary()); m.put("hireDate", e.getHireDate().toString());
        m.put("skills", e.getSkills());
        return m;
    }
}
