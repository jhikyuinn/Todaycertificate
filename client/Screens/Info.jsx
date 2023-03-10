import { useEffect ,useState} from "react";
import { Modal,StyleSheet,View,Text,TouchableOpacity} from "react-native";
import AuroraButton from '../Components/AuroraButton';
import Ionicons from 'react-native-vector-icons/Ionicons';
import { TextInput } from "react-native-gesture-handler";
import Axios from "axios"

const Info = ({userinfo,navigation})=>{

    console.log("π΄"+(JSON.stringify(userinfo)))
    console.log("π΄"+(userinfo.userinfo[0].name))

    const [review,setReview] = useState('');

    const [isModalVisible, setIsModalVisible] = useState(false);

    const reviewacquisition = () => {
        if(userinfo.userinfo[0].acquisition_certificate !== " " && userinfo.userinfo[0].acquisition_certificate !== null){
            modalopen()
        }
        else{
            window.alert("νκΈ°λ₯Ό μμ±ν  μκ²©μ¦μ΄ μμ΅λλ€.")
        }

    }


    const reviewacquisitioncomplete = async() => {
        await Axios.get(`http://10.10.1.58:4000/api/reviewinsert`,{
            params: {
                'id':userinfo.userinfo[0].id,
                'review':review
            }
        }).then(res=>{
            setReview("")
            window.alert("μμ±μ μλ£νμ΅λλ€.")
            modalopen()
        })
        .catch(error=> console.log(error));
      }

    function modalopen(){
        setIsModalVisible(!isModalVisible)
    }

    return(
        <View style={styles.home}>
            <Modal
                style={styles.center}
                useNativeDriver={true}
                transparent={true}
                visible={isModalVisible}
            >
                <View style={styles.modalview}>
                   
                    <Text>{userinfo.userinfo[0].acquisition_certificate} νκΈ°</Text>
                    <TextInput value={review} onChangeText={text => setReview(text)} style={styles.input} placeholderTextColor="rgba(255,255,255,0.7)" placeholder="νκΈ° μμ±" />  
                    <AuroraButton  width="70%" height={30} bgcolor="rgba(255,255,255,0.7)" buttonFunction={reviewacquisitioncomplete} fontSize={15} fontWeight="bold" text="μμ± μλ£" color={"black"} outline={false}/>
               </View>
            </Modal>

        <Ionicons style={{marginTop:"10%"}} name="person-circle-outline" size={200}  color="black" />
        <Text style={{fontSize:16,fontWeight:"bold"}}>{userinfo.userinfo[0].id}</Text>
        <Text style={{fontSize:16,fontWeight:"bold"}}>μ΄λ¦:{userinfo.userinfo[0].name}</Text>
        
        {userinfo.userinfo[0].acquisition_certificate !== " " && userinfo.userinfo[0].acquisition_certificate !== null?
            <View style={styles.rowcontainer}>
                <Ionicons name="ios-ribbon-outline" size={25}/>
                <Text style={{fontSize:16,fontWeight:"bold"}}>{userinfo.userinfo[0].acquisition_certificate}</Text>
            </View>:
            <></>
        }

    <AuroraButton  width="70%" height={50} bgcolor="rgba(255,255,255,0.7)" fontSize={15} fontWeight="bold" text="νμμ λ³΄ μμ " color={"black"} outline={false}/>
    <AuroraButton  width="70%" height={50} bgcolor="rgba(255,255,255,0.7)" buttonFunction={reviewacquisition} fontSize={15} fontWeight="bold" text="μκ²©μ¦ νκΈ° μμ±" color={"black"} outline={false}/>

        </View>
    )
}

export default Info

const styles = StyleSheet.create({
    center:{
        alignItems:"center",
        justifyContent:"center",
    },
    modalview:{
        position:"fixed",
        top: "25%",
        left: "5%",
        
        alignItems:"center",
        justifyContent:"center",
        width:"90%",
        height:"50%",
        backgroundColor:"white",
        padding:5
      },
    home:{
        alignItems: "center",
        width:"100%",
        height:"100%",
    },
    topSection:{
        alignItems:'center',
        justifyContent:'center',
        height:'5%',
    },
    input:{
        height:200,
        width:'100%',
        backgroundColor:'lightgrey',
        color:"black",
        borderRadius:10,
        borderwidth: "10px",
        borderColor:'black',
    },
    body:{
        flex:1,
        backgroundColor:'white',
        flexDirection:'column',
        justifyContent:'space-between'
    },
    bottomSection:{
        width:'100%',
        height:'95%',
        backgroundColor:'rgba(44,117,84,0.8)',
    },
    rowcontainer:{
        flexDirection:"row",
    },
})