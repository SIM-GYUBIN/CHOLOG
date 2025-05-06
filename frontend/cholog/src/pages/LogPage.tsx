import { useParams } from 'react-router-dom'

const LogPage = () => {
  const { id } = useParams()

  return (
    <div>
      LogPage (ID: {id})
    </div>
  )
}

export default LogPage
