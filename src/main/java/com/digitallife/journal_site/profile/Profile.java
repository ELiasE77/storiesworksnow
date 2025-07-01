package com.digitallife.journal_site.profile;

import com.digitallife.journal_site.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    private String gender;

    @NotNull @Min(1) @Max(120)
    private Integer age;

    @NotBlank
    private String height;

    @NotBlank
    private String nationality;

    @NotBlank
    private String hair;

    @NotBlank @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String hobbies;

    @NotBlank @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String persona;

    /** The AI-generated 300–500-word “persona feature.” */
    @Lob
    @Column(name = "persona_feature", columnDefinition = "LONGTEXT")
    private String personaFeature;

    // ——— Constructors ———

    public Profile() { }

    // ——— Getters only for the PK ———
    public Long getId() {
        return id;
    }

    // ——— Association setter ———
    /** Required so @MapsId can pick up the FK as PK. */
    public void setUser(User user) {
        this.user = user;
        // no need to set id here; @MapsId will do it for you
    }
    public User getUser() {
        return user;
    }

    // ——— Rest of your bean properties ———
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getHair() { return hair; }
    public void setHair(String hair) { this.hair = hair; }

    public String getHobbies() { return hobbies; }
    public void setHobbies(String hobbies) { this.hobbies = hobbies; }

    public String getPersona() { return persona; }
    public void setPersona(String persona) { this.persona = persona; }

    public String getPersonaFeature() { return personaFeature; }
    public void setPersonaFeature(String personaFeature) { this.personaFeature = personaFeature; }
}
