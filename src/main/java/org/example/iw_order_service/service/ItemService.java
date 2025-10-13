package org.example.iw_order_service.service;
import lombok.RequiredArgsConstructor;
import org.example.iw_order_service.exception.ItemNotFoundException;
import org.example.iw_order_service.mapper.ItemMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.example.iw_order_service.dto.ItemResponse;
import org.example.iw_order_service.repository.ItemRepository;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    public Page<ItemResponse> getAll(Pageable pageable) {
        return itemRepository.findAll(pageable).map(itemMapper::toItemResponse);
    }
    public ItemResponse getById(Long id) {
        return itemMapper.toItemResponse(itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException("Item not found + " + id)) );
    }
}
