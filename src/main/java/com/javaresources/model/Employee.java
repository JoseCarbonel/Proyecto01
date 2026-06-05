package com.javaresources.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class Employee {

    public static final ImmutableList<String> VALID_ROLES = ImmutableList.of(
        "DEVELOPER", "MANAGER", "ANALYST", "QA", "DEVOPS", "ARCHITECT"
    );

    private final int    id;
    private final String name;
    private final String email;
    private final String department;
    private final String role;
    private final double salary;
    private final LocalDate hireDate;
    private final List<String> skills;

    private Employee(Builder b) {
        this.id = b.id; this.name = b.name; this.email = b.email;
        this.department = b.department; this.role = b.role;
        this.salary = b.salary; this.hireDate = b.hireDate;
        this.skills = ImmutableList.copyOf(b.skills);
    }

    public int getId()              { return id; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getDepartment()   { return department; }
    public String getRole()         { return role; }
    public double getSalary()       { return salary; }
    public LocalDate getHireDate()  { return hireDate; }
    public List<String> getSkills() { return skills; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id).add("name", name).add("email", email)
            .add("department", department).add("role", role)
            .add("salary", salary).omitNullValues().toString();
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee e)) return false;
        return id == e.id && Objects.equals(email, e.email);
    }
    @Override public int hashCode() { return Objects.hash(id, email); }

    public static class Builder {
        private int id; private String name; private String email;
        private String department; private String role;
        private double salary; private LocalDate hireDate = LocalDate.now();
        private List<String> skills = ImmutableList.of();

        public Builder id(int id) {
            Preconditions.checkArgument(id > 0, "ID debe ser positivo, recibido: %s", id);
            this.id = id; return this;
        }
        public Builder name(String name) {
            Preconditions.checkArgument(StringUtils.isNotBlank(name), "Nombre vacío");
            this.name = StringUtils.capitalize(name.trim().toLowerCase()); return this;
        }
        public Builder email(String email) {
            Preconditions.checkArgument(StringUtils.isNotBlank(email) && email.contains("@"),
                "Email inválido: %s", email);
            this.email = email.toLowerCase().trim(); return this;
        }
        public Builder department(String d) {
            Preconditions.checkArgument(StringUtils.isNotBlank(d), "Departamento vacío");
            this.department = d.toUpperCase().trim(); return this;
        }
        public Builder role(String role) {
            String r = StringUtils.upperCase(StringUtils.trimToEmpty(role));
            Preconditions.checkArgument(VALID_ROLES.contains(r),
                "Rol inválido '%s'. Permitidos: %s", role, VALID_ROLES);
            this.role = r; return this;
        }
        public Builder salary(double s) {
            Preconditions.checkArgument(s >= 0, "Salario negativo: %s", s);
            this.salary = s; return this;
        }
        public Builder hireDate(LocalDate d) {
            this.hireDate = Preconditions.checkNotNull(d); return this;
        }
        public Builder skills(List<String> s) {
            this.skills = (s != null) ? s : ImmutableList.of(); return this;
        }
        public Employee build() {
            Preconditions.checkState(id > 0, "ID requerido");
            Preconditions.checkState(name != null, "Nombre solicitado");
            Preconditions.checkState(email != null, "Email solicitado");
            return new Employee(this);
        }
    }
}
