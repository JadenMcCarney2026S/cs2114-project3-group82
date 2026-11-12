package spotter;
import java.util.NoSuchElementException;
public class LinkedQueue<T>
{
    private Node<T> front;
    private Node<T> rear;
    private int size;
    
    private static class Node<T>
    {
        private T data;
        private Node<T> next;
        
        public Node(T data)
        {
            this.data = data;
            this.next = null;
        }
    }
    
    public void enqueue(T newEntry)
    {
        if (newEntry == null)
        {
            throw new IllegalArgumentException("Cannot enqueue null");
        }
        
        Node<T> newNode = new Node<>(newEntry);
        
        if (isEmpty())
        {
            front = newNode;
        }
        else
        {
            rear.next = newNode;
        }
        
        rear = newNode;
        size++;
    }
    
    public T dequeue()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Queue is empty");
        }
        
        T removedData = front.data;
        front = front.next;
        size--;
        
        if (front == null)
        {
            rear = null;
        }
        
        return removedData;
    }
    
    public T peek()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Queue is empty");
        }
        
        return front.data;
    }
    
    public boolean isEmpty()
    {
        return size == 0;
    }
    
    public int getSize()
    {
        return size;
    }
}
