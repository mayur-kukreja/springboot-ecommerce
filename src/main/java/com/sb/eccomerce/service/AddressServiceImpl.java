package com.sb.eccomerce.service;

import com.sb.eccomerce.exceptions.ResourceNotFoundException;
import com.sb.eccomerce.model.Address;
import com.sb.eccomerce.model.User;
import com.sb.eccomerce.payload.AddressDTO;
import com.sb.eccomerce.repositries.AddressRepository;
import com.sb.eccomerce.repositries.UserRepository;
import com.sb.eccomerce.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user){
        Address address = modelMapper.map(addressDTO, Address.class);
        address.setUser(user);
        List<Address> addressList = user.getAddress();
        addressList.add(address);
        user.setAddress(addressList);
        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    public List<AddressDTO> getAddresses(){
        List<Address> addresses = addressRepository.findAll();
        return addresses.stream().map(address -> modelMapper.map(address, AddressDTO.class)).toList();
    }

    public AddressDTO getAddressesById(Long addressId){
        Address address = addressRepository.findById(addressId).
                orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));
        return modelMapper.map(address, AddressDTO.class);
    }

    public List<AddressDTO> getUserAddresses(User user){
        List<Address> addresses = user.getAddress();
        return addresses.stream().map(address ->
                modelMapper.map(address, AddressDTO.class)).toList();
    }

    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO){
        Address addressFromDatabase = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        addressFromDatabase.setCity(addressDTO.getCity());
        addressFromDatabase.setPincode(addressDTO.getPincode());
        addressFromDatabase.setState(addressDTO.getState());
        addressFromDatabase.setCountry(addressDTO.getCountry());
        addressFromDatabase.setStreet(addressDTO.getStreet());
        addressFromDatabase.setBuildingName(addressDTO.getBuildingName());

        Address updatedAddress = addressRepository.save(addressFromDatabase);

        User user = addressFromDatabase.getUser();
        user.getAddress().removeIf(address -> address.getAddressId().equals(addressId));
        user.getAddress().add(updatedAddress);
        userRepository.save(user);

        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    public String deleteAddress(Long addressId){
        Address addressFromDatabase = addressRepository.findById(addressId).orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId ));

        User user = addressFromDatabase.getUser();
        user.getAddress().removeIf(address -> address.getAddressId().equals(addressId));
        userRepository.save(user);

        addressRepository.delete(addressFromDatabase);

        return "Address deleted successfully with addressId: "+addressId;
    }


}
