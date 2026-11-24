package com.yep.platform.user;

import com.yep.platform.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "profiles")
public class Profile extends BaseEntity {
    @Id private Long userId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name", nullable = false) private String lastName;
    private String phone;
    @Column(nullable = false) private String country;
    private Short age;
    @Column(name = "photo_file_id") private Long photoFileId;
    private String bio, skills, interests, education, organization;

    protected Profile() {}
    public Profile(User user, String firstName, String lastName, String phone, String country, Short age) {
        this.user = user; this.firstName = firstName; this.lastName = lastName;
        this.phone = phone; this.country = country; this.age = age;
    }
    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { lastName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getCountry() { return country; }
    public void setCountry(String v) { country = v; }
    public Short getAge() { return age; }
    public void setAge(Short v) { age = v; }
    public Long getPhotoFileId() { return photoFileId; }
    public void setPhotoFileId(Long v) { photoFileId = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { bio = v; }
    public String getSkills() { return skills; }
    public void setSkills(String v) { skills = v; }
    public String getInterests() { return interests; }
    public void setInterests(String v) { interests = v; }
    public String getEducation() { return education; }
    public void setEducation(String v) { education = v; }
    public String getOrganization() { return organization; }
    public void setOrganization(String v) { organization = v; }
}
