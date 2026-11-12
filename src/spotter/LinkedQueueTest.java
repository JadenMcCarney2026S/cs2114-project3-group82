package spotter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;

class LinkedQueueTest
{

    @Test
    void testEnqueue()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        
        queue.enqueue("Grace");
        queue.enqueue("Phoebe");
        
        assertEquals(2, queue.getSize());
        assertEquals("Grace", queue.peek());
        assertFalse(queue.isEmpty());
    }
    
    @Test 
    void testDequeue()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        
        queue.enqueue("Grace");
        queue.enqueue("Phoebe");
        
        assertEquals("Grace", queue.dequeue());
        assertEquals("Phoebe", queue.peek());
        assertEquals(1, queue.getSize());
    }
    
    @Test
    void testPeek()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        
        queue.enqueue("Grace");
        queue.enqueue("Phoebe");
        
        assertEquals("Grace", queue.peek());
        assertEquals(2, queue.getSize());
        assertEquals("Grace", queue.peek());
    }
    
    @Test
    void testIsEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        
        assertTrue(queue.isEmpty());
        queue.enqueue("Grace");
        assertFalse(queue.isEmpty());
        
        queue.dequeue();
        assertTrue(queue.isEmpty());
    }
    
    @Test
    void testEnqueueNull()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        
        assertThrows(IllegalArgumentException.class, () ->
        {
            queue.enqueue(null);
        });
        
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.getSize());
    }
    
    @Test
    void testDequeueEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        
        assertThrows(NoSuchElementException.class, () -> {
            queue.dequeue();
        });
        
        assertTrue(queue.isEmpty());
    }
    
    @Test
    void testPeekEmpty()
    {
        LinkedQueue<String> queue = new LinkedQueue<>(); 
        assertThrows(NoSuchElementException.class, () -> {
            queue.peek();
        });
        
        assertTrue(queue.isEmpty());
    }
    
    @Test
    void testGetSize()
    {
        LinkedQueue<String> queue = new LinkedQueue<>();
        assertEquals(0, queue.getSize());
        
        queue.enqueue("Grace");
        queue.enqueue("Phoebe");
        
        assertEquals(2, queue.getSize());
        
        queue.dequeue();
        
        assertEquals(1, queue.getSize());
    }

}
