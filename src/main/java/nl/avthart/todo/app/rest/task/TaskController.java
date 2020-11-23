package nl.avthart.todo.app.rest.task;

import java.security.Principal;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import nl.avthart.todo.app.configuration.Endpoint;
import nl.avthart.todo.app.domain.task.commands.TaskCommand;
import nl.avthart.todo.app.domain.task.commands.TaskCommandComplete;
import nl.avthart.todo.app.domain.task.commands.TaskCommandCreate;
import nl.avthart.todo.app.domain.task.commands.TaskCommandModifyTitle;
import nl.avthart.todo.app.domain.task.commands.TaskCommandStar;
import nl.avthart.todo.app.domain.task.commands.TaskCommandUnstar;
import nl.avthart.todo.app.query.task.TaskEntry;
import nl.avthart.todo.app.query.task.TaskEntryRepository;
import nl.avthart.todo.app.rest.task.requests.TaskRequestCreate;
import nl.avthart.todo.app.rest.task.requests.TaskRequestModifyTitle;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.IdentifierFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author albert
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController implements Endpoint.Controller {

    private final IdentifierFactory identifierFactory = IdentifierFactory.getInstance();

    private final TaskEntryRepository taskEntryRepository;

    private final SimpMessageSendingOperations messagingTemplate;

    private final CommandGateway commandGateway;

    @GetMapping
    public @ResponseBody
    Page<TaskEntry> findAll( Principal principal,
                             @RequestParam(required = false, defaultValue = "false") boolean completed,
                             Pageable pageable ) {
        return taskEntryRepository.findByUsernameAndCompleted( principal.getName(), completed, pageable );
    }

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public void create( Principal principal, @RequestBody @Valid TaskRequestCreate request ) {
        sendAndWait( TaskCommandCreate.builder()
                             .id( identifierFactory.generateIdentifier() )
                             .username( principal.getName() )
                             .title( request.getTitle() )
                             .build() );
    }

    @PostMapping("/{identifier}/title") // IMO: should be PATCH, but not supported by current Angular version
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void modifyTitle( @PathVariable String identifier, @RequestBody @Valid TaskRequestModifyTitle request ) {
        sendAndWait( TaskCommandModifyTitle.builder()
                             .id( identifier )
                             .title( request.getTitle() )
                             .build() );
    }

    @PostMapping("/{identifier}/complete") // IMO: should be PATCH, but not supported by current Angular version
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void complete( @PathVariable String identifier ) {
        sendAndWait( new TaskCommandComplete( identifier ) );
    }

    @PostMapping("/{identifier}/star") // IMO: should be PATCH, but not supported by current Angular version
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void star( @PathVariable String identifier ) {
        sendAndWait( new TaskCommandStar( identifier ) );
    }

    @PostMapping("/{identifier}/unstar") // IMO: should be PATCH, but not supported by current Angular version
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unstar( @PathVariable String identifier ) {
        sendAndWait( new TaskCommandUnstar( identifier ) );
    }

    @DeleteMapping("/{identifier}/delete")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete( @PathVariable String identifier ) {
        // TODO: sendAndWait( new TaskCommandDelete( identifier ) );
    }

    @PutMapping("/{identifier}/restore")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Endpoint.Admin
    public void restore( @PathVariable String identifier ) {
        // TODO: sendAndWait( new TaskCommandRestore( identifier ) );
    }

    @ExceptionHandler
    public void handleException( Principal principal, Throwable exception ) {
        messagingTemplate.convertAndSendToUser( principal.getName(), "/queue/errors", exception.getMessage() );
    }

    private void sendAndWait( TaskCommand command ) {
        commandGateway.sendAndWait( command );
    }
}
