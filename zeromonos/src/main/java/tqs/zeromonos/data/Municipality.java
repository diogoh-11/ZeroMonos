package tqs.zeromonos.data;

import jakarta.persistence.*;

@Entity
@Table(name = "municipalities")
public class Municipality {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String name;

    public Municipality() {}

    public Municipality(String name) {
        this.name = name;
    }

    // getters/setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

}


