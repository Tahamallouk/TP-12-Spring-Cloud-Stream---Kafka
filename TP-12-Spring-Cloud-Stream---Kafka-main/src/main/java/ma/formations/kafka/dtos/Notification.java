package ma.formations.kafka.dtos;

import lombok.*;
import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString
public class Notification {
    private String code;
    private Date date;
    private Double authorizedSpeed;
    private Double currentSpeed;
    private String registrationNumber;
}
