# GitHub Copilot Instructions for Panorama Dash API

Este projeto segue uma arquitetura simples e limpa baseada nos princípios SOLID. As instruções abaixo devem ser seguidas ao gerar código.

## Arquitetura do Projeto

O projeto utiliza uma arquitetura em camadas simples:

```
src/main/java/mp/invest/
├── resource/           # Camada de apresentação (REST endpoints)
├── service/            # Camada de negócio (lógica de negócio)
├── repository/         # Camada de dados (acesso a dados)
├── model/              # Entidades e modelos de domínio
├── dto/                # Data Transfer Objects
├── config/             # Configurações da aplicação
└── exception/          # Exceções personalizadas
```

## Princípios SOLID

### S - Single Responsibility Principle (Princípio da Responsabilidade Única)

Cada classe deve ter apenas uma responsabilidade:

- **Resources**: Apenas receber requisições HTTP e delegar para services
- **Services**: Conter apenas lógica de negócio
- **Repositories**: Apenas operações de acesso a dados
- **DTOs**: Apenas transferência de dados entre camadas

```java
// Correto: Resource apenas delega para o service
@Path("/users")
public class UserResource {
    @Inject
    UserService userService;

    @GET
    public List<UserDTO> getAll() {
        return userService.findAll();
    }
}

// Correto: Service contém a lógica de negócio
@ApplicationScoped
public class UserService {
    @Inject
    UserRepository userRepository;

    public List<UserDTO> findAll() {
        return userRepository.findAll()
            .stream()
            .map(this::toDTO)
            .toList();
    }
}
```

### O - Open/Closed Principle (Princípio Aberto/Fechado)

Classes devem estar abertas para extensão, mas fechadas para modificação:

```java
// Correto: Use interfaces para permitir extensões
public interface NotificationService {
    void send(String message, String recipient);
}

@ApplicationScoped
@Named("email")
public class EmailNotificationService implements NotificationService {
    public void send(String message, String recipient) {
        // Implementação de email
    }
}

@ApplicationScoped
@Named("sms")
public class SmsNotificationService implements NotificationService {
    public void send(String message, String recipient) {
        // Implementação de SMS
    }
}
```

### L - Liskov Substitution Principle (Princípio da Substituição de Liskov)

Subtipos devem ser substituíveis por seus tipos base:

```java
// Correto: Subclasses podem ser usadas onde a classe base é esperada
public abstract class BaseEntity {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters e setters
}

public class User extends BaseEntity {
    private String name;
    private String email;
    // Campos específicos
}
```

### I - Interface Segregation Principle (Princípio da Segregação de Interface)

Interfaces devem ser específicas para cada cliente:

```java
// Correto: Interfaces pequenas e específicas
public interface Readable<T> {
    T findById(Long id);
    List<T> findAll();
}

public interface Writable<T> {
    T save(T entity);
    void delete(Long id);
}

// Um repository pode implementar as interfaces necessárias
public interface UserRepository extends Readable<User>, Writable<User> {
    Optional<User> findByEmail(String email);
}
```

### D - Dependency Inversion Principle (Princípio da Inversão de Dependência)

Dependa de abstrações, não de implementações concretas:

```java
// Correto: Injete interfaces, não implementações
@ApplicationScoped
public class OrderService {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Inject
    public OrderService(PaymentService paymentService, 
                        NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public void processOrder(Order order) {
        paymentService.process(order.getPayment());
        notificationService.send("Pedido processado", order.getCustomerEmail());
    }
}
```

## Convenções de Código

### Nomenclatura

- **Classes**: PascalCase (ex: `UserService`, `OrderRepository`)
- **Métodos**: camelCase (ex: `findById`, `calculateTotal`)
- **Constantes**: UPPER_SNAKE_CASE (ex: `MAX_RETRY_COUNT`)
- **Pacotes**: lowercase (ex: `mp.invest.service`)

### Estrutura de Arquivos

- Uma classe pública por arquivo
- Nome do arquivo igual ao nome da classe
- Agrupar classes relacionadas no mesmo pacote

### Injeção de Dependência

- Preferir injeção por construtor
- Usar `@Inject` do Jakarta CDI
- Anotar beans com `@ApplicationScoped` para singleton

```java
@ApplicationScoped
public class ProductService {

    private final ProductRepository productRepository;

    @Inject
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

### DTOs e Validação

- Usar DTOs para entrada e saída de dados
- Aplicar validações com Bean Validation (Jakarta Validation)
- Separar DTOs de request e response quando necessário

```java
public record CreateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password
) {}

public record UserResponse(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt
) {}
```

### Tratamento de Exceções

- Criar exceções específicas do domínio
- Usar `@ExceptionMapper` para converter exceções em respostas HTTP

```java
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}

@Provider
public class EntityNotFoundExceptionMapper 
        implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse(e.getMessage()))
            .build();
    }
}
```

## Testes

- Escrever testes unitários para services
- Escrever testes de integração para resources
- Usar mocks para dependências externas

```java
@QuarkusTest
class UserResourceTest {

    @Test
    void shouldReturnUserById() {
        given()
            .when().get("/users/1")
            .then()
            .statusCode(200)
            .body("name", is("John Doe"));
    }
}
```

## Tecnologias Utilizadas

- **Quarkus**: Framework Java
- **Jakarta REST (JAX-RS)**: API REST
- **CDI (Contexts and Dependency Injection)**: Injeção de dependência
- **SmallRye OpenAPI**: Documentação da API
- **JUnit 5 + REST Assured**: Testes

## Boas Práticas Adicionais

1. **Imutabilidade**: Preferir objetos imutáveis quando possível (usar `record` para DTOs)
2. **Fail Fast**: Validar entradas o mais cedo possível
3. **Logs Significativos**: Usar logging estruturado para debugging
4. **Documentação**: Documentar endpoints com OpenAPI annotations
5. **Configuração Externa**: Usar `application.yml` para configurações
