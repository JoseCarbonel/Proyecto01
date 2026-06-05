package com.javaresources.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.*;
import com.javaresources.model.Employee;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final AtomicInteger idGen = new AtomicInteger(100);

    private final Cache<Integer, Employee> cache = CacheBuilder.newBuilder()
        .maximumSize(500)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .recordStats()
        .build();

    private final ListMultimap<String, Employee> byDept = ArrayListMultimap.create();
    private final Table<String, String, List<Employee>> orgTable = HashBasedTable.create();
    private final Map<Integer, Employee> registry = new LinkedHashMap<>();

    @PostConstruct
    public void loadSampleData() {
        List<Employee> samples = List.of(
            new Employee.Builder().id(1).name("Maria Garcia").email("m.garcia@empresa.com")
                .department("IT").role("DEVELOPER").salary(5500.0)
                .hireDate(LocalDate.of(2021, 3, 15))
                .skills(List.of("Java", "Spring Boot", "Docker")).build(),
            new Employee.Builder().id(2).name("Carlos Mendoza").email("c.mendoza@empresa.com")
                .department("IT").role("ARCHITECT").salary(8200.0)
                .hireDate(LocalDate.of(2019, 1, 10))
                .skills(List.of("Java", "AWS", "Kubernetes")).build(),
            new Employee.Builder().id(3).name("Ana Torres").email("a.torres@empresa.com")
                .department("QA").role("QA").salary(4200.0)
                .hireDate(LocalDate.of(2022, 7, 1))
                .skills(List.of("Selenium", "JUnit", "Cucumber")).build(),
            new Employee.Builder().id(4).name("Luis Ramirez").email("l.ramirez@empresa.com")
                .department("IT").role("DEVOPS").salary(6800.0)
                .hireDate(LocalDate.of(2020, 5, 20))
                .skills(List.of("Docker", "Jenkins", "Terraform")).build(),
            new Employee.Builder().id(5).name("Sofia Paredes").email("s.paredes@empresa.com")
                .department("MANAGEMENT").role("MANAGER").salary(9500.0)
                .hireDate(LocalDate.of(2018, 2, 8))
                .skills(List.of("Scrum", "Jira", "Confluence")).build(),
            new Employee.Builder().id(6).name("Diego Vasquez").email("d.vasquez@empresa.com")
                .department("IT").role("ANALYST").salary(4800.0)
                .hireDate(LocalDate.of(2023, 1, 5))
                .skills(List.of("SQL", "Power BI", "Python")).build(),
            new Employee.Builder().id(7).name("Valeria Castillo").email("v.castillo@empresa.com")
                .department("QA").role("QA").salary(4500.0)
                .hireDate(LocalDate.of(2022, 9, 12))
                .skills(List.of("Postman", "TestNG", "Appium")).build()
        );
        samples.forEach(this::addEmployee);
        idGen.set(10);
        log.info("Datos de muestra cargados: {} empleados", registry.size());
    }

    public Employee addEmployee(Employee e) {
        MDC.put("operation", "ADD_EMPLOYEE");
        try {
            registry.put(e.getId(), e);
            cache.put(e.getId(), e);
            byDept.put(e.getDepartment(), e);
            List<Employee> cell = orgTable.get(e.getDepartment(), e.getRole());
            if (cell == null) { cell = new ArrayList<>(); orgTable.put(e.getDepartment(), e.getRole(), cell); }
            cell.add(e);
            log.info("Empleado registrado id={} nombre='{}' dept={}", e.getId(), e.getName(), e.getDepartment());
            return e;
        } finally { MDC.remove("operation"); }
    }

    public Employee createAndAdd(String name, String email, String dept, String role, double salary, List<String> skills) {
        int id = idGen.incrementAndGet();
        Employee e = new Employee.Builder()
            .id(id).name(name).email(email).department(dept)
            .role(role).salary(salary).skills(skills).build();
        return addEmployee(e);
    }

    public boolean deleteEmployee(int id) {
        Employee e = registry.remove(id);
        if (e == null) return false;
        cache.invalidate(id);
        byDept.remove(e.getDepartment(), e);
        List<Employee> cell = orgTable.get(e.getDepartment(), e.getRole());
        if (cell != null) cell.remove(e);
        log.info("Empleado eliminado id={}", id);
        return true;
    }

    public Optional<Employee> findById(int id) {
        Employee cached = cache.getIfPresent(id);
        if (cached != null) { log.debug("Cache HIT id={}", id); return Optional.of(cached); }
        Employee fromReg = registry.get(id);
        if (fromReg != null) cache.put(id, fromReg);
        return Optional.ofNullable(fromReg);
    }

    public List<Employee> findByDepartment(String dept) {
        return ImmutableList.copyOf(byDept.get(StringUtils.upperCase(StringUtils.trimToEmpty(dept))));
    }

    public List<Employee> findByRole(String role) {
        String r = StringUtils.upperCase(StringUtils.trimToEmpty(role));
        return registry.values().stream().filter(e -> e.getRole().equals(r)).collect(Collectors.toList());
    }

    public List<Employee> getTopEarners(int n) {
        Ordering<Employee> order = Ordering
            .from(Comparator.comparingDouble(Employee::getSalary).reversed())
            .compound(Comparator.comparing(Employee::getName));
        List<Employee> sorted = order.sortedCopy(registry.values());
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    public Map<String, Integer> getHeadcountByDepartment() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String d : byDept.keySet()) map.put(d, byDept.get(d).size());
        return map;
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("hits",    cache.stats().hitCount());
        s.put("misses",  cache.stats().missCount());
        s.put("hitRate", Math.round(cache.stats().hitRate() * 100));
        s.put("size",    cache.size());
        return s;
    }

    public Table<String, String, List<Employee>> getOrgTable() {
        return ImmutableTable.copyOf(orgTable);
    }

    public Collection<Employee> getAll() { return ImmutableList.copyOf(registry.values()); }

    public List<String> getValidRoles() { return Employee.VALID_ROLES; }
}
