package avro;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
    @JsonProperty
    public String firstName;
  
    @JsonProperty
    public String lastName;
  
    @JsonProperty
    public short age;
  
    public User() {}
  
    public User(String firstName, String lastName, short age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }
}